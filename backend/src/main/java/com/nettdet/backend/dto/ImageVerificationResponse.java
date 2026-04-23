package com.nettdet.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task payload returned for one Fotografen image-verification task.")
public record ImageVerificationResponse(
    @Schema(description = "Task id used when validating the answer.", example = "17")
    Long taskId,
    @Schema(description = "Task description shown to the pupil.", example = "Vurder om bildet virker ekte eller manipulert.")
    String taskDescription,
    @Schema(description = "File object id for the task image.", example = "42")
    Long fileId,
    @Schema(description = "Available justification options for the pupil to choose from.")
    List<String> justifications
) {}
