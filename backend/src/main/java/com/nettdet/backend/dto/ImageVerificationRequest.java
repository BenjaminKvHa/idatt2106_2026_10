package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.ImageCategory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Answer payload for one Fotografen image-verification task.")
public record ImageVerificationRequest(
    @Schema(description = "Selected category for the image task.", example = "AI_GENERATED")
    ImageCategory selectedCategory,
    @Schema(description = "Selected justification text chosen by the pupil.", example = "Bildet viser tegn på manipulering")
    String selectedJustification
) {}
