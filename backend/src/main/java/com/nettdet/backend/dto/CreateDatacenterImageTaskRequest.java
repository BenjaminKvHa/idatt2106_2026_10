package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.enums.ImageCategory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for creating a Datasenteret task in image-verification format. The image bytes are sent as a separate `file` multipart part.")
public record CreateDatacenterImageTaskRequest(
    @Schema(description = "Task description shown to the pupil.", example = "Er dette bildet ekte, KI-generert eller manipulert?")
    String taskDescription,

    @Schema(description = "Correct category for the image.", example = "AI_GENERATED")
    ImageCategory correctCategory,

    @Schema(description = "Correct justification string.", example = "Bildet viser tegn på manipulering")
    String correctJustification,

    @Schema(description = "List of possible justification options.")
    List<String> justifications,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String falseDescription
) {}
