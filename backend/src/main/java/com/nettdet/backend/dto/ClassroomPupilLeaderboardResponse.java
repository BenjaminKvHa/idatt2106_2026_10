package com.nettdet.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pupil leaderboard for a single classroom.")
public record ClassroomPupilLeaderboardResponse(
    @Schema(description = "Unique classroom code. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Classroom display name.", example = "8A")
    String classroomName,
    @Schema(description = "Sorted pupil leaderboard entries for the classroom.")
    List<ClassroomPupilLeaderboardEntryResponse> entries
) {
}
