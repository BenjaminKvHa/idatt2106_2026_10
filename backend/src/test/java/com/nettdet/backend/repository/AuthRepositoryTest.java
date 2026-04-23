package com.nettdet.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.RefreshToken;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.support.PostgresDataJpaTest;

@PostgresDataJpaTest
class AuthRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Finds an account by exact email")
    @Test
    void findByEmail_whenAccountExists_returnsMatchingAccount() {
        insertAccount("teacher@example.com", "Teacher Name", "TEACHER");

        Optional<Account> account = accountRepository.findByEmail("teacher@example.com");

        assertTrue(account.isPresent());
        assertEquals("teacher@example.com", account.orElseThrow().getEmail());
        assertEquals("Teacher Name", account.orElseThrow().getLegalName());
        assertEquals(AccountRole.TEACHER, account.orElseThrow().getAccountRole());
    }

    @DisplayName("Finds a stored refresh token by hashed token value")
    @Test
    void findByRefreshToken_whenTokenExists_returnsMatchingToken() {
        insertAccount("pupil@example.com", "Pupil Name", "PUPIL");
        insertRefreshToken("pupil@example.com", "hashed-refresh-token");

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByRefreshToken("hashed-refresh-token");

        assertTrue(refreshToken.isPresent());
        assertEquals("hashed-refresh-token", refreshToken.orElseThrow().getRefreshToken());
        assertEquals("pupil@example.com", refreshToken.orElseThrow().getAccount().getEmail());
    }

    @DisplayName("Deletes all refresh tokens for an account")
    @Test
    void deleteByAccount_whenAccountHasTokens_removesOnlyThatAccountsTokens() {
        insertAccount("pupil@example.com", "Pupil Name", "PUPIL");
        insertAccount("other@example.com", "Other Pupil", "PUPIL");
        insertRefreshToken("pupil@example.com", "first-token");
        insertRefreshToken("pupil@example.com", "second-token");
        insertRefreshToken("other@example.com", "other-token");
        Account account = accountRepository.findByEmail("pupil@example.com").orElseThrow();

        refreshTokenRepository.deleteByAccount(account);
        refreshTokenRepository.flush();

        assertTrue(refreshTokenRepository.findByRefreshToken("first-token").isEmpty());
        assertTrue(refreshTokenRepository.findByRefreshToken("second-token").isEmpty());
        assertTrue(refreshTokenRepository.findByRefreshToken("other-token").isPresent());
    }

    private void insertAccount(String email, String legalName, String role) {
        jdbcTemplate.update(
                "INSERT INTO account (email, password_data, legal_name, account_role) VALUES (?, ?, ?, ?::account_role_enum)",
                email,
                "encoded-password",
                legalName,
                role);
    }

    private void insertRefreshToken(String email, String refreshToken) {
        jdbcTemplate.update(
                "INSERT INTO refresh_token (email, refresh_token) VALUES (?, ?)",
                email,
                refreshToken);
    }
}
