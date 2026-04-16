package com.nettdet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClassroomRequest(
    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be less than 256 characters")
    String name,

    String description
) {
}
