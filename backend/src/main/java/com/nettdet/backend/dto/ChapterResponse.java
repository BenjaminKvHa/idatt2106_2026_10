package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic chapter metadata.")
public record ChapterResponse(
    @Schema(description = "Unique chapter id.", example = "1")
    Long id,
    @Schema(description = "Chapter display name.", example = "Nyhetskvartalet")
    String chapterName
) {}
