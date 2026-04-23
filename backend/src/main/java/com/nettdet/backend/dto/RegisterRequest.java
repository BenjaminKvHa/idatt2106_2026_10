package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.AccountRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for creating a new pupil or teacher account.")
public record RegisterRequest(
        @Schema(description = "Unique account email.", example = "pupil@example.com")
        String email,
        @Schema(description = "Plain-text password.", example = "Test1234")
        String password,
        @Schema(description = "Real full name of the user.", example = "Pupil Name")
        String legalName,
        @Schema(description = "Role to create for the account.", example = "PUPIL")
        AccountRole role) {
}
