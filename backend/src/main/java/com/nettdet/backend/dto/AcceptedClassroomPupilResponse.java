package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Accepted pupil summary returned to teachers for a classroom member list.")
public record AcceptedClassroomPupilResponse(
    @Schema(description = "Unique pupil identifier used in later teacher endpoints.", example = "pupil1@example.com")
    String accountEmail,
    @Schema(description = "Pupil legal name from the account profile.", example = "Pupil Name")
    String legalName,
    @Schema(description = "Pupil username shown in the UI.", example = "detektiv1")
    String username,
    @Schema(description = "Saved avatar object key. The frontend can derive `/api/avatar-images/{fileName}` from it.", example = "avatar-images/boy_hair1_costume1.png", nullable = true)
    String avatarObjectKey,
    @Schema(description = "Total XP for the pupil using the same calculation as the classroom leaderboard.", example = "240")
    long totalXp
) {
}
