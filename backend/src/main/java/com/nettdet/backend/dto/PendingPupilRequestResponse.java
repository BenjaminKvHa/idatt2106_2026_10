package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pending pupil request waiting for teacher approval.")
public record PendingPupilRequestResponse(
    @Schema(description = "Email of the pupil who requested to join.", example = "pupil@example.com")
    String accountEmail,
    @Schema(description = "Legal name of the pupil account.", example = "Pupil Name")
    String legalName,
    @Schema(description = "Pupil's requested classroom display username.", example = "detektiv1")
    String username,
    @Schema(description = "Avatar object key used with GET /avatar-images/{fileName}. Null when no avatar is set.", example = "avatar-images/boy_hair1_costume1.png", nullable = true)
    String avatarObjectKey
) {
}
