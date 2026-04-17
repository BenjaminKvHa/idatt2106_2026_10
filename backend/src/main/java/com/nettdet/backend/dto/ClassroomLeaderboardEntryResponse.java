package com.nettdet.backend.dto;

public record ClassroomLeaderboardEntryResponse(
    String classroomCode,
    String classroomName,
    long totalXp
) {
}
