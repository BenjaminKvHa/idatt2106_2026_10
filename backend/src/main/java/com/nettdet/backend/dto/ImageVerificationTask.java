package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.enums.ImageCategory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Internal/full Fotografen task structure including correct answer data.")
public record ImageVerificationTask(
    @Schema(description = "Task description shown to the pupil.", example = "Vurder om bildet virker ekte eller manipulert.")
    String taskDescription,
    @Schema(description = "File object id for the image to verify.", example = "42")
    Long fileId,
    @Schema(description = "Correct category for the task.", example = "AI_GENERATED")
    ImageCategory correctCategory,
    @Schema(description = "Correct justification string for the task.", example = "Bildet viser tegn på manipulering")
    String correctJustification,
    @Schema(description = "Possible justification options.")
    List<String> justifications,
    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,
    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String falseDescription
) {}
