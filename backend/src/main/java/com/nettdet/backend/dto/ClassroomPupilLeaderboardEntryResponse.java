package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One pupil entry inside a classroom leaderboard.")
public record ClassroomPupilLeaderboardEntryResponse(
    @Schema(description = "Pupil username.", example = "detektiv1")
    String username,
    @Schema(description = "Total XP for the pupil inside the classroom.", example = "240")
    long totalXp
) {
}
