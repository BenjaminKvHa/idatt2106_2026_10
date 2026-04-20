package com.nettdet.backend.dto;
package com.nettdet.backend.domain.enums;

public record RegisterRequest(String email, String password, String legalName, AccountRole role) {
}
