package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One backend-managed tip for a pupil chapter, saved to the notebook when newly received.")
public record ChapterTipResponse(
    @Schema(description = "Chapter id the tip belongs to.", example = "1")
    Long chapterId,
    @Schema(description = "Tip text. Null only when no tips are configured for this chapter.", example = "Se nøye på kilden før du bestemmer deg.", nullable = true)
    String tip,
    @Schema(description = "Whether this request added the returned tip to the pupil notebook.", example = "true")
    boolean newlyAdded,
    @Schema(description = "Whether the pupil has all configured tips for this chapter after this request.", example = "false")
    boolean allTipsReceived
) {
}
