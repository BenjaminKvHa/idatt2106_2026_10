package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for creating a Fotografen (Image Verification) task. The image bytes are sent as a separate `file` multipart part.")
public record CreateImageVerificationTaskRequest(
    @Schema(description = "Chapter ID to associate the task with.", example = "1")
    Long chapterId,

    @Schema(description = "Difficulty level for the task.", example = "EASY")
    TaskDifficulty difficulty,

    @Schema(description = "Task description shown to the pupil.", example = "Vurder om bildet virker ekte eller manipulert.")
    String taskDescription,

    @Schema(description = "Correct category for the image.", example = "AI_GENERATED")
    ImageCategory correctCategory,

    @Schema(description = "Correct justification string.", example = "Bildet viser tegn på manipulering i bakgrunnen")
    String correctJustification,

    @Schema(description = "List of possible justification options.")
    List<String> justifications,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String falseDescription
) {}
