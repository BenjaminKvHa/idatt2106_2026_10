package com.nettdet.backend.dto;

import java.util.List;

public record ChapterNotebookResponse(
    Long chapterId,
    List<String> tips,
    String ownNotes
) {
}
