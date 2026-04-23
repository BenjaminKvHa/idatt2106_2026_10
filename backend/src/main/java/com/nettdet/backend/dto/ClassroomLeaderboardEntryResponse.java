package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One classroom entry in the global classroom leaderboard.")
public record ClassroomLeaderboardEntryResponse(
    @Schema(description = "Unique classroom code. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Display name of the classroom.", example = "8A")
    String classroomName,
    @Schema(description = "Total XP accumulated by confirmed pupils in the classroom.", example = "420")
    long totalXp
) {
}
