package com.nettdet.backend.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Approved weekly mystery visible to confirmed pupils in the classroom.")
public record PupilWeeklyMysteryResponse(
    @Schema(description = "Unique weekly mystery id.", example = "1")
    Long id,
    @Schema(description = "Stored file object id. Use GET /objects/{fileObjectId} to load the file.", example = "12")
    Long fileObjectId,
    @Schema(description = "Comment submitted by the pupil.", example = "Jeg fant dette mystiske bildet på nett.")
    String comment,
    @Schema(description = "Time the weekly mystery was submitted.", example = "2026-04-23T10:15:30")
    LocalDateTime createdAt
) {
}
