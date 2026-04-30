package com.nettdet.backend.util;

import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.nettdet.backend.domain.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Utility component for handling JSON Web Tokens (JWT).
 * <p>
 * This class provides methods for generating, parsing, and validating tokens 
 * used for stateless authentication. It utilizes HMAC SHA algorithms for 
 * cryptographic signing.
 * </p>
 * 
 * @author YourName
 * @since 1.0.0
 */
@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    /** The signing key derived from the application secret. */
    private SecretKey key;

    /** 
     * The Base64 encoded secret string injected from application properties.
     * Required to be at least 256 bits for HS256 compatibility.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Decodes the Base64 encoded secret and generates a HMAC SHA key.
     * 
     * @return A {@link SecretKey} suitable for signing and verifying JWTs.
     */
    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Post-construction hook to initialize the cryptographic key.
     * This ensures the key is ready for use immediately after dependency injection.
     */
    @PostConstruct
    private void init() {
        this.key = getKey();
    }

    /**
     * Generates a signed JWT for a specific {@link Account}.
     * <p>
     * The token contains the user's email as the subject, along with custom claims 
     * for 'role' and 'legalName'. The token is set to expire 15 minutes after issuance.
     * </p>
     * 
     * @param account The account entity containing user details.
     * @return A compact URL-safe JWT string, or {@code null} if an encoding error occurs.
     */
    public String generateToken(Account account) {
        // Token lifetime: 15 minutes
        Date expiration = new Date(System.currentTimeMillis() + 15 * 60 * 1000);

        try {
            return Jwts.builder()
                        .subject(account.getEmail())
                        .claim("role", account.getAccountRole())
                        .claim("legalName", account.getLegalName())
                        .issuedAt(new Date())
                        .expiration(expiration)
                        .signWith(key)
                        .compact();
        } catch (JwtException error) {
            logger.error("Failed to sign JWT for user {}: {}", account.getEmail(), error.getMessage());
            return null;
        }
    }

    /**
     * Parses and verifies the JWT payload.
     * <p>
     * This method validates the signature and ensures the token has not been tampered with.
     * </p>
     * 
     * @param jwtToken The raw JWT string to parse.
     * @return The {@link Claims} object containing the token's payload.
     * @throws JwtException If the signature is invalid, the token is expired, or malformed.
     */
    public Claims extractAllClaims(String jwtToken) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(jwtToken)
            .getPayload();
    }

    /**
     * Validates whether a JWT is structurally sound, correctly signed, and not expired.
     * 
     * @param jwtToken The raw JWT string.
     * @return {@code true} if the token is valid for authentication; {@code false} otherwise.
     */
    public boolean isTokenValid(String jwtToken) {
        try {
            if (jwtToken == null || jwtToken.isEmpty()) {
                logger.warn("Validation failed: JWT token is null or empty");
                return false;
            }
            extractAllClaims(jwtToken);
            return true;
        } catch (JwtException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the subject (email) from the provided token.
     * 
     * @param jwtToken The raw JWT string.
     * @return The email address stored in the subject field.
     */
    public String extractEmail(String jwtToken) {
        return extractAllClaims(jwtToken).getSubject();
    }

    /**
     * Extracts the custom "role" claim from the token.
     * 
     * @param jwtToken The raw JWT string.
     * @return The user's role as a {@link String}.
     */
    public String extractUserRole(String jwtToken) {
        return extractAllClaims(jwtToken).get("role", String.class);
    }

    /**
     * Extracts the custom "legalName" claim from the token.
     * 
     * @param jwtToken The raw JWT string.
     * @return The user's legal name.
     */
    public String extractLegalName(String jwtToken) {
        return extractAllClaims(jwtToken).get("legalName", String.class);
    }
}