package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher home-page overview for one classroom the authenticated teacher belongs to.")
public record TeacherClassroomOverviewResponse(
    @Schema(description = "Unique classroom code. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Classroom display name.", example = "8A")
    String classroomName,
    @Schema(description = "Total classroom score used in the teacher overview.", example = "420")
    long totalScore,
    @Schema(description = "Global classroom rank among all classrooms.", example = "2")
    int globalRank
) {
}
