package com.nettdet.backend.security;

import java.io.Serializable;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Represents the authenticated user principal extracted from a JWT.
 * 
 * <p>This record stores essential user information required for authorization and 
 * identity management within the security context. It implements {@link AuthenticatedPrincipal}
 * to integrate with Spring Security's authentication model and {@link Serializable} 
 * for compatibility with session-less architectures.
 * 
 * @param legalName the full legal name of the user
 * @param email     the unique email address identifying the account
 * @param role      the security role assigned to the user
 */
public record JwtUserPrincipal(
    String legalName, 
    String email, 
    String role) implements AuthenticatedPrincipal, Serializable {
    
    /**
     * Returns the name of the principal, mapped to the legal name.
     *
     * @return the legal name of the authenticated user
     */
    @Override
    public String getName() {
        return this.legalName;
    }
}
