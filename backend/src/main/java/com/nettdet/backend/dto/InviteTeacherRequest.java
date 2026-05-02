package com.nettdet.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for adding another teacher to a classroom.")
public record InviteTeacherRequest(
    @Schema(description = "Email of the teacher account to add.", example = "other.teacher@example.com")
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email
) {
}
