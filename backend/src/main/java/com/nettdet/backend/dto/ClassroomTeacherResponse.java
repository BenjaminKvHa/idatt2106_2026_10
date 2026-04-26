package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher attached to a classroom.")
public record ClassroomTeacherResponse(
    @Schema(description = "Teacher's account email.", example = "teacher.multi@example.com")
    String email,
    @Schema(description = "Teacher's legal name.", example = "Teacher Multi")
    String legalName
) {}
