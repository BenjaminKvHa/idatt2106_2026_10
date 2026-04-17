package com.nettdet.backend.dto;

import java.util.List;

public record ClassroomPupilLeaderboardResponse(
    String classroomCode,
    String classroomName,
    List<ClassroomPupilLeaderboardEntryResponse> entries
) {
}
