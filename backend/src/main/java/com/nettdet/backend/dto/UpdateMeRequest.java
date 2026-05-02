package com.nettdet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
    @NotBlank(message = "username is required")
    @Size(max = 30, message = "username must be at most 30 characters")
    String username
) {
}
