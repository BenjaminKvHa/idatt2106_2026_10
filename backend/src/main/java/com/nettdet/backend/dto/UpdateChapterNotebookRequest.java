package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pupil-editable notebook content for a chapter.")
public record UpdateChapterNotebookRequest(
    @Schema(description = "Free-form pupil notes. Tips are managed by the backend and are not updated through this request.", example = "Dette må jeg huske til neste gang.")
    String ownNotes
) {
}
