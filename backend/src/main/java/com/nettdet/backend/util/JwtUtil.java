package com.nettdet.backend.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private SecretKey key;

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Decodes the Base64 encoded secret and generates a HMAC SHA key.
     * * @return a {@link SecretKey} for signing and verifying tokens.
     */
    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Initializes the signing key after the component has been constructed and
     * dependencies injected.
     */
    @PostConstruct
    private void init() {
        this.key = getKey();
    }

    public String generateToken(String email) {
        Date expiration = new Date(System.currentTimeMillis() + 15 * 60 * 1000);

        try {
            String jwt = Jwts.builder()
                        .subject(email)
                        .claim("role", "a user role")
                        .claim("legalName", "a users legal name")
                        .issuedAt(new Date())
                        .expiration(expiration)
                        .signWith(key)
                        .compact();

            return jwt;
        } catch (JwtException error) {
            logger.error("Error creating JWT: {}", error.getMessage());
            return null;
        }
    }


    /**
     * Decrypts and parses the JWT payload using the configured secret key.
     *
     * @param jwtToken the raw JWT string.
     * @return the {@link Claims} object containing the token payload.
     * @throws JwtException if the token is invalid or the signature fails verification.
     */
    public Claims extractAllClaims(String jwtToken) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(jwtToken)
            .getPayload();
    }

    /**
     * Validates a JWT string by attempting to parse its claims.
     *
     * @param jwtToken the raw JWT string.
     * @return {@code true} if the token is well-formed, correctly signed, and not expired;
     * {@code false} otherwise.
     */
    public boolean isTokenValid(String jwtToken) {
        try {
            if (jwtToken == null || jwtToken.isEmpty()) {
                logger.error("JWT token is null or empty");
                return false;
            }
            extractAllClaims(jwtToken);
            return true;
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String jwtToken) {
        return extractAllClaims(jwtToken).getSubject();
    }

    public String extractUserRole(String jwtToken) {
        return extractAllClaims(jwtToken).get("Role", String.class);
    }

    public String extractLegalName(String jwtToken) {
        return extractAllClaims(jwtToken).get("legalName", String.class);
    }


}
