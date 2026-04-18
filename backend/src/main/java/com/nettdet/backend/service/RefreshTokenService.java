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



@Service 
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private final RefreshTokenUtil util;
    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenUtil util, RefreshTokenRepository repository) {
        this.util = util;
        this.repository = repository;
    }

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

    @Transactional
    public ResponseCookie rotateRefreshToken(RefreshToken existing) {
        Account account = existing.getAccount();
        logger.info("Rotating refresh token for user: {}", account.getLegalName());
        repository.delete(existing);
        return createAndStoreRefreshToken(account);
    }

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

    @Transactional
    public ResponseCookie logout(String incomingToken) {
        RefreshToken token = getStoredRefreshToken(incomingToken);
        Account account = token.getAccount();
        logger.info("Logging out user: {}", account.getEmail());
        repository.delete(token);
        return util.createExpiredRefreshTokenCookie();
    }

    public void revokeAllTokens(Account account) {
        logger.info("Revoking all refresh tokens for user: {}", account.getLegalName());
        repository.deleteByAccount(account);
        logger.info("All refresh tokens revoked for user: {}", account.getLegalName());
    }
}
