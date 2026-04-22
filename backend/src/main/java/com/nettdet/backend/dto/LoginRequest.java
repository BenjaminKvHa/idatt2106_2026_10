package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials used to authenticate an existing account.")
public record LoginRequest(
        @Schema(description = "Account email.", example = "teacher@example.com")
        String email,
        @Schema(description = "Plain-text password.", example = "Test1234")
        String password) {
}
