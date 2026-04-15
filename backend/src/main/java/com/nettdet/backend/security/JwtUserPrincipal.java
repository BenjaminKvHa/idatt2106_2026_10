package com.nettdet.backend.security;

import java.io.Serializable;

import org.springframework.security.core.AuthenticatedPrincipal;

public record JwtUserPrincipal(
    String legalName, 
    String email, 
    String role) implements AuthenticatedPrincipal, Serializable {
    
    @Override
    public String getName() {
        return this.legalName;
    }
}
