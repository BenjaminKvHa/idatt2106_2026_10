package com.nettdet.backend.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after a pupil submits a weekly mystery.")
public record CreateWeeklyMysteryResponse(
    @Schema(description = "Unique weekly mystery id.", example = "1")
    Long id,
    @Schema(description = "Stored file object id. Use GET /objects/{fileObjectId} to load the file when authorized.", example = "12")
    Long fileObjectId,
    @Schema(description = "Pupil comment submitted with the file.", example = "Jeg fant dette mystiske bildet på nett.")
    String comment,
    @Schema(description = "Time the weekly mystery was submitted.", example = "2026-04-23T10:15:30")
    LocalDateTime createdAt,
    @Schema(description = "Whether the teacher has approved the weekly mystery.", example = "false")
    boolean approved
) {
}
