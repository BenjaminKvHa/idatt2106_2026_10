package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic classroom metadata.")
public record ClassroomResponse(
    @Schema(description = "Unique classroom code used in later requests. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Classroom display name.", example = "8A")
    String classroomName,
    @Schema(description = "Optional classroom description.", example = "Mandag formiddag", nullable = true)
    String classroomDescription
) {
}
