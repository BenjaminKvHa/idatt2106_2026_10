package com.nettdet.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Utility component for secure generation, hashing, and cookie-packaging of Refresh Tokens.
 * <p>
 * This class provides cryptographic helper methods to support the Refresh Token lifecycle, 
 * ensuring that tokens are generated with high entropy, stored securely via one-way hashing, 
 * and transmitted to the client using security-hardened HTTP cookies.
 * </p>
 */
@Component
public class RefreshTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(7);

    /**
     * Generates a high-entropy, cryptographically secure random string to be used as a refresh token.
     * <p>
     * Uses {@link SecureRandom} to produce 32 bytes of randomness, which are then Base64 
     * URL-encoded for safe transmission in HTTP headers or cookies.
     * </p>
     *
     * @return a unique, URL-safe plaintext refresh token string.
     */
    public String generateRefreshToken() {
        byte[] bytearr = new byte[32];
        new SecureRandom().nextBytes(bytearr); // fills the byte array random bytes with the operating systems entropy source

        logger.info("token value for refresh token created succesfully");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytearr); // converts it into string of readable ASCII values
    }

    /**
     * Packages a plaintext refresh token into a security-hardened {@link ResponseCookie}.
     * <p>
     * The cookie is configured with the following security attributes:
     * <ul>
     * <li><b>HttpOnly:</b> Prevents JavaScript access to mitigate Cross-Site Scripting (XSS).</li>
     * <li><b>SameSite (Strict):</b> Prevents the browser from sending the cookie with cross-site requests to mitigate CSRF.</li>
     * <li><b>Path Scoping:</b> Limits cookie transmission to the {@code /api/auth} endpoint tree.</li>
     * <li><b>Max-Age:</b> Sets a 7-day expiration policy.</li>
     * </ul>
     * </p>
     *
     * @param tokenValue the plaintext refresh token to be stored in the cookie.
     * @return a configured {@link ResponseCookie} ready to be added to the HTTP response.
     */
    public ResponseCookie createRefreshTokenCookie(String tokenValue) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenValue)
                                .httpOnly(true)
                                .maxAge(REFRESH_TOKEN_LIFETIME)
                                .path("/api/auth")
                                .sameSite("Strict")
                                .build();

        logger.info("refreshToken cookie created successfully");
        return cookie;
    }

    public ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                             .httpOnly(true)
                             .maxAge(0)
                             .path("/api/auth")
                             .sameSite("Strict")
                             .build();
    }

    /**
     * Creates a one-way SHA-256 hash of a plaintext token.
     * <p>
     * This method is used to hash tokens before database storage. By storing only 
     * the hash, the system remains secure even if the database is compromised, 
     * as the original plaintext token cannot be easily reversed.
     * </p>
     *
     * @param token the plaintext token to hash.
     * @return a Base64 URL-encoded string representing the SHA-256 hash.
     * @throws RuntimeException if the SHA-256 algorithm is not supported by the environment.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // returns a message digest object that implements the specified hash algorithm
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8)); // transforms the token into its correspondign array of bytes, and hashes w ith SHA-256
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes); // encodes the bytes into base64 and returns as string
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
