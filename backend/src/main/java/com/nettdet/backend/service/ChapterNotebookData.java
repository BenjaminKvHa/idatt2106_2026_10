package com.nettdet.backend.service;

import java.util.List;

public record ChapterNotebookData(
    List<String> tips,
    String ownNotes
) {
    public static ChapterNotebookData empty() {
        return new ChapterNotebookData(List.of(), "");
    }
}
