package com.nettdet.backend.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.RefreshToken;
import com.nettdet.backend.repository.RefreshTokenRepository;
import com.nettdet.backend.util.RefreshTokenUtil;



/**
 * Persistence and rotation logic for opaque refresh tokens used by the JWT auth flow.
 * <p>
 * Refresh tokens are issued as plaintext values inside an HTTP-only cookie, but stored
 * server-side as a SHA-hashed digest produced by {@link RefreshTokenUtil#hashToken(String)};
 * the plaintext is therefore never persisted. Lookups always rehash the incoming cookie
 * value before querying the database.
 * <p>
 * This service implements the security-critical pieces of the flow:
 * <ul>
 *     <li>{@link #getStoredRefreshToken(String)} validates an incoming token, rejecting unknown
 *         and expired entries (expired entries are also deleted as a side effect);</li>
 *     <li>{@link #rotateRefreshToken(RefreshToken)} deletes the consumed token and issues a
 *         fresh one, implementing single-use rotation;</li>
 *     <li>{@link #createAndStoreRefreshToken(Account)} mints a brand-new token and returns the
 *         {@link ResponseCookie} the controller should set;</li>
 *     <li>{@link #logout(String)} consumes a token and returns an immediately-expired cookie
 *         so the client is forced to drop it;</li>
 *     <li>{@link #revokeAllTokens(Account)} invalidates every outstanding session for a user.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private final RefreshTokenUtil util;
    private final RefreshTokenRepository repository;

    /**
     * Constructs a {@code RefreshTokenService} over the token utility and persistence layer.
     *
     * @param util       generates, hashes, and packages refresh-token cookies
     * @param repository repository for the {@code refresh_token} table
     */
    public RefreshTokenService(RefreshTokenUtil util, RefreshTokenRepository repository) {
        this.util = util;
        this.repository = repository;
    }

    /**
     * Validates an incoming refresh-token cookie value and returns the matching stored entity.
     * <p>
     * The incoming plaintext is hashed and looked up; rows whose {@code expiresAt} has passed
     * are deleted before the failure is reported, so expired entries do not linger.
     *
     * @param incomingToken the raw token value taken from the cookie
     * @return the matching {@link RefreshToken} entity
     * @throws BadCredentialsException if the token is unknown to the database or expired
     */
    @Transactional
    public RefreshToken getStoredRefreshToken(String incomingToken) {
        String hashed = util.hashToken(incomingToken);
        RefreshToken token = repository.findByRefreshToken(hashed).orElseThrow(() -> {
            logger.warn("Refresh token validation failed: token not found in database");
            return new BadCredentialsException("Invalid refresh token");
        });

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Refresh token validation failed: token expired for user {}", token.getAccount().getEmail());
            repository.delete(token);
            throw new BadCredentialsException("Refresh token expired");
        }

        return token;
    }

    /**
     * Single-use rotation: deletes the consumed token and issues a fresh one for the same account.
     * <p>
     * The caller is expected to have validated the {@code existing} token via
     * {@link #getStoredRefreshToken(String)} immediately beforehand.
     *
     * @param existing the token row that has just been consumed
     * @return the {@link ResponseCookie} carrying the newly minted refresh token
     */
    @Transactional
    public ResponseCookie rotateRefreshToken(RefreshToken existing) {
        Account account = existing.getAccount();
        logger.info("Rotating refresh token for user: {}", account.getLegalName());
        repository.delete(existing);
        return createAndStoreRefreshToken(account);
    }

    /**
     * Mints a new refresh token for the given account, persists its hashed digest, and returns
     * the {@link ResponseCookie} the controller should attach to the response.
     * <p>
     * The plaintext value lives only inside the returned cookie; only the hash is stored.
     *
     * @param user the account for whom the token is being issued
     * @return the cookie the controller should set on the response
     * @throws IllegalArgumentException if {@code user} is {@code null}
     */
    public ResponseCookie createAndStoreRefreshToken(Account user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        logger.info("Creating and storing refresh token for user: {}", user.getLegalName());
        
        String plaintext = util.generateRefreshToken();
        String hashed = util.hashToken(plaintext);

        RefreshToken entity = new RefreshToken();
        entity.setAccount(user);
        entity.setRefreshToken(hashed);
        repository.save(entity);
        logger.info("Refresh token stored in database for user: {}", user.getLegalName());

        return util.createRefreshTokenCookie(plaintext);
    }

    /**
     * Consumes the supplied refresh token and returns an immediately-expired cookie so the
     * client drops its copy.
     *
     * @param incomingToken the raw token value taken from the cookie
     * @return a cookie that, when set on the response, clears the client's refresh-token cookie
     * @throws BadCredentialsException if the token is unknown or expired
     */
    @Transactional
    public ResponseCookie logout(String incomingToken) {
        RefreshToken token = getStoredRefreshToken(incomingToken);
        Account account = token.getAccount();
        logger.info("Logging out user: {}", account.getEmail());
        repository.delete(token);
        return util.createExpiredRefreshTokenCookie();
    }

    /**
     * Deletes every refresh token associated with the given account, signing the user out of
     * all active sessions.
     *
     * @param account the account whose tokens should all be revoked
     */
    public void revokeAllTokens(Account account) {
        logger.info("Revoking all refresh tokens for user: {}", account.getLegalName());
        repository.deleteByAccount(account);
        logger.info("All refresh tokens revoked for user: {}", account.getLegalName());
    }
}
