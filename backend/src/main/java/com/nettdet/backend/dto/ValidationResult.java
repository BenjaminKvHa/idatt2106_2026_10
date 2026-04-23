package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Validation result returned after a pupil submits an answer to a chapter task.")
public record ValidationResult(
    @Schema(description = "Whether the submitted answer was correct.", example = "true")
    boolean correct,
    @Schema(description = "Feedback message explaining the result.", example = "Riktig! Du oppdaget den troverdige saken.")
    String feedback,
    @Schema(description = "Updated number of attempted tasks in the current run.", example = "3")
    int currentRunAttempts,
    @Schema(description = "Updated number of correct answers in the current run.", example = "2")
    int currentRunCorrect,
    @Schema(description = "Whether the current run has now been completed.", example = "false")
    boolean runCompleted,
    @Schema(description = "Fotografen-only: whether the submitted category was correct. Null for other chapter types.", example = "true", nullable = true)
    Boolean categoryCorrect,
    @Schema(description = "Fotografen-only: whether the submitted justification was correct. Null for other chapter types.", example = "false", nullable = true)
    Boolean justificationCorrect
) {}
