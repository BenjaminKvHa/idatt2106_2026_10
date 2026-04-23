package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pending pupil request waiting for teacher approval.")
public record PendingPupilRequestResponse(
    @Schema(description = "Email of the pupil who requested to join.", example = "pupil@example.com")
    String accountEmail,
    @Schema(description = "Legal name of the pupil account.", example = "Pupil Name")
    String legalName
) {
}
