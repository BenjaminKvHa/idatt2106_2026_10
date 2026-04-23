package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Answer payload for one Fotografen image-verification task.")
public record ImageVerificationRequest(
    @Schema(description = "Selected category index for the image task.", example = "1")
    int selectedCategory,
    @Schema(description = "Selected justification text chosen by the pupil.", example = "Bildet viser tegn på manipulering")
    String selectedJustification
) {}
