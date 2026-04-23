package com.nettdet.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notebook data for a pupil and chapter.")
public record ChapterNotebookResponse(
    @Schema(description = "Chapter id the notebook belongs to.", example = "2")
    Long chapterId,
    @Schema(description = "Read-only tips saved by the backend or game flow.")
    List<String> tips,
    @Schema(description = "Free-form notes editable by the pupil.", example = "Pupilen skrev dette selv")
    String ownNotes
) {
}
