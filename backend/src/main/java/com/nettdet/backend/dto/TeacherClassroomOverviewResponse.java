package com.nettdet.backend.dto;

public record TeacherClassroomOverviewResponse(
    String classroomCode,
    String classroomName,
    long totalScore,
    int globalRank
) {
}
