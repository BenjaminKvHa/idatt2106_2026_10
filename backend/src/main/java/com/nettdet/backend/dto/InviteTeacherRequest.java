package com.nettdet.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteTeacherRequest(
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email
) {
}
