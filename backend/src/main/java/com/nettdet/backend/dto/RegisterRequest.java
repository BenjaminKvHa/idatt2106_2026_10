package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.AccountRole;

public record RegisterRequest(String email, String password, String legalName, AccountRole role) {
}
