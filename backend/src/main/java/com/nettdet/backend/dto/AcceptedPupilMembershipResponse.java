package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result returned when a teacher approves a pending pupil request.")
public record AcceptedPupilMembershipResponse(
    @Schema(description = "Email of the approved pupil.", example = "pupil@example.com")
    String accountEmail,
    @Schema(description = "Pupil legal name from the account.", example = "Pupil Name")
    String legalName,
    @Schema(description = "Whether the membership is now confirmed by a teacher.", example = "true")
    boolean membershipConfirmedByTeacher
) {
}
