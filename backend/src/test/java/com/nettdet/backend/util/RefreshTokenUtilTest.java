package com.nettdet.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenUtilTest {

    private final RefreshTokenUtil refreshTokenUtil = new RefreshTokenUtil();

    @DisplayName("Generates URL-safe refresh tokens from 32 bytes of entropy")
    @Test
    void generateRefreshToken_whenCalled_returnsUrlSafeTokenWithExpectedLength() {
        String token = refreshTokenUtil.generateRefreshToken();

        assertEquals(43, token.length());
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
    }

    @DisplayName("Hashes refresh tokens deterministically without storing the plaintext value")
    @Test
    void hashToken_whenSameTokenIsHashed_returnsSameNonPlaintextHash() {
        String token = "plain-refresh-token";

        String firstHash = refreshTokenUtil.hashToken(token);
        String secondHash = refreshTokenUtil.hashToken(token);

        assertEquals(firstHash, secondHash);
        assertNotEquals(token, firstHash);
        assertFalse(firstHash.isBlank());
    }

    @DisplayName("Creates refresh-token cookies with HttpOnly path and strict SameSite settings")
    @Test
    void createRefreshTokenCookie_whenCalled_returnsSecureRefreshCookie() {
        ResponseCookie cookie = refreshTokenUtil.createRefreshTokenCookie("plain-token");

        assertEquals("refresh_token", cookie.getName());
        assertEquals("plain-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(RefreshTokenUtil.REFRESH_TOKEN_LIFETIME, cookie.getMaxAge());
    }

    @DisplayName("Creates expired refresh-token cookies for logout")
    @Test
    void createExpiredRefreshTokenCookie_whenCalled_returnsExpiredCookie() {
        ResponseCookie cookie = refreshTokenUtil.createExpiredRefreshTokenCookie();

        assertEquals("refresh_token", cookie.getName());
        assertEquals("", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(0, cookie.getMaxAge().getSeconds());
    }
}
