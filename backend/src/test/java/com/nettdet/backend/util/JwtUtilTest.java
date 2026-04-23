package com.nettdet.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.enums.AccountRole;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");
    }

    @DisplayName("Generates a valid JWT containing account email, role, and legal name")
    @Test
    void generateToken_whenAccountIsValid_containsExpectedClaims() {
        Account account = account("teacher@example.com", "Teacher Name", AccountRole.TEACHER);

        String token = jwtUtil.generateToken(account);

        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("teacher@example.com", jwtUtil.extractEmail(token));
        assertEquals("TEACHER", jwtUtil.extractUserRole(token));
        assertEquals("Teacher Name", jwtUtil.extractLegalName(token));
    }

    @DisplayName("Rejects null JWT tokens")
    @Test
    void isTokenValid_whenTokenIsNull_returnsFalse() {
        boolean valid = jwtUtil.isTokenValid(null);

        assertFalse(valid);
    }

    @DisplayName("Rejects blank JWT tokens")
    @Test
    void isTokenValid_whenTokenIsBlank_returnsFalse() {
        boolean valid = jwtUtil.isTokenValid("");

        assertFalse(valid);
    }

    @DisplayName("Rejects tampered JWT tokens")
    @Test
    void isTokenValid_whenTokenIsTampered_returnsFalse() {
        String token = jwtUtil.generateToken(account("pupil@example.com", "Pupil Name", AccountRole.PUPIL));
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        boolean valid = jwtUtil.isTokenValid(tamperedToken);

        assertFalse(valid);
    }

    private Account account(String email, String legalName, AccountRole role) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(legalName);
        account.setAccountRole(role);
        account.setPasswordData("encoded-password");
        return account;
    }
}
