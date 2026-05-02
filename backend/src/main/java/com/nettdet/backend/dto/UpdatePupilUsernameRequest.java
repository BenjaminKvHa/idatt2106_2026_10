package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Teacher request for updating a pupil's classroom username.")
public record UpdatePupilUsernameRequest(
    @Schema(description = "New username shown for the pupil inside the classroom.", example = "nydetektiv")
    @NotBlank(message = "username is required")
    @Size(max = 30, message = "username must be at most 30 characters")
    String username
) {
}
