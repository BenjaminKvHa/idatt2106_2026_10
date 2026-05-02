package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials used to authenticate an existing account.")
public record LoginRequest(
        @Schema(description = "Account email.", example = "teacher@example.com")
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,
        @Schema(description = "Plain-text password.", example = "Test1234")
        String password) {
}
