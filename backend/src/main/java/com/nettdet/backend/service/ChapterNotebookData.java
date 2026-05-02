package com.nettdet.backend.service;

import java.util.List;

/**
 * Data transfer object representing the contents of a pupil's notebook for a specific chapter.
 * 
 * <p>Contains both system-provided tips that the pupil has unlocked and their own 
 * personal notes for the chapter.
 *
 * @param tips     a list of unlocked tip strings for the chapter
 * @param ownNotes the pupil's personal notes for the chapter
 */
public record ChapterNotebookData(
    List<String> tips,
    String ownNotes
) {
    /**
     * Creates an empty notebook data object.
     * 
     * @return a {@link ChapterNotebookData} with no tips and empty notes
     */
    public static ChapterNotebookData empty() {
        return new ChapterNotebookData(List.of(), "");
    }
}
