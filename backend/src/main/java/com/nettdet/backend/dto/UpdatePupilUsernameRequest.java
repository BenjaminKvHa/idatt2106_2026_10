package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher request for updating a pupil's classroom username.")
public record UpdatePupilUsernameRequest(
    @Schema(description = "New username shown for the pupil inside the classroom.", example = "nydetektiv")
    String username
) {
}
