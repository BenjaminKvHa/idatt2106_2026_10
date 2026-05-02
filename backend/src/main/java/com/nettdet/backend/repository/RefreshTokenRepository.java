package com.nettdet.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.RefreshToken;

/**
 * Repository interface for managing {@link RefreshToken} entities.
 * 
 * <p>Handles the lifecycle of refresh tokens used for session management.
 * Includes methods for retrieving tokens by their string value and 
 * performing bulk deletions for specific accounts during logout or security events.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token record by its string representation.
     *
     * @param refreshToken the literal refresh token string
     * @return an {@link Optional} containing the refresh token entity if found
     */
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    /**
     * Deletes all refresh tokens associated with a specific account.
     * This effectively invalidates all active long-lived sessions for the user.
     *
     * @param account the account whose tokens should be removed
     */
    @Modifying
    @Transactional
    void deleteByAccount(Account account);
}
