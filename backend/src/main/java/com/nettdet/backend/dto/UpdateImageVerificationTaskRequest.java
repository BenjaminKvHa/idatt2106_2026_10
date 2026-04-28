package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for updating an Image Verification task. Used for both Fotografen and Datasenteret image-format tasks. Send a `file` multipart part to replace the image; omit it to keep the existing one.")
public record UpdateImageVerificationTaskRequest(
    @Schema(description = "Difficulty level for the task. Ignored for Datasenteret tasks (always HARD).", example = "EASY")
    TaskDifficulty difficulty,

    @Schema(description = "Task description shown to the pupil.")
    String taskDescription,

    @Schema(description = "Correct category for the image.", example = "AI_GENERATED")
    ImageCategory correctCategory,

    @Schema(description = "Correct justification string.")
    String correctJustification,

    @Schema(description = "List of possible justification options.")
    List<String> justifications,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String falseDescription
) {}
