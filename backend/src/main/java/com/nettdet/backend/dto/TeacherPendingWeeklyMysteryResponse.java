package com.nettdet.backend.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pending weekly mystery visible only to teachers in the classroom.")
public record TeacherPendingWeeklyMysteryResponse(
    @Schema(description = "Unique weekly mystery id.", example = "1")
    Long id,
    @Schema(description = "Stored file object id. Teachers in the classroom can load it with GET /objects/{fileObjectId}.", example = "12")
    Long fileObjectId,
    @Schema(description = "Email of the pupil who submitted the mystery.", example = "pupil@example.com")
    String pupilEmail,
    @Schema(description = "Legal name of the pupil who submitted the mystery.", example = "Pupil Name")
    String pupilLegalName,
    @Schema(description = "Comment submitted by the pupil.", example = "Jeg fant dette mystiske bildet på nett.")
    String comment,
    @Schema(description = "Time the weekly mystery was submitted.", example = "2026-04-23T10:15:30")
    LocalDateTime createdAt
) {
}
