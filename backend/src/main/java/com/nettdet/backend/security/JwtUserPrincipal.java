package com.nettdet.backend.security;

import java.io.Serializable;

import org.springframework.security.core.AuthenticatedPrincipal;

public record JwtUserPrincipal(
    String username, 
    String email, 
    String role) implements AuthenticatedPrincipal, Serializable {
    
    @Override
    public String getName() {
        return this.username;
    }
}
