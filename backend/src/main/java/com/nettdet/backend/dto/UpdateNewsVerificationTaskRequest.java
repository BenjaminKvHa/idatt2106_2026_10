package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.CorrectFileSlot;
import com.nettdet.backend.domain.enums.TaskDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for updating a News Verification task. Used for both Nyhetskvartalet and Datasenteret news-format tasks. Send `file1` and/or `file2` multipart parts to replace either image; omit to keep existing.")
public record UpdateNewsVerificationTaskRequest(
    @Schema(description = "Difficulty level for the task. Ignored for Datasenteret tasks (always HARD).", example = "MEDIUM")
    TaskDifficulty difficulty,

    @Schema(description = "Task description shown to the pupil.")
    String taskDescription,

    @Schema(description = "Which of the two files is the credible source.", example = "FILE_1")
    CorrectFileSlot correctFileSlot,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String incorrectDescription,

    @Schema(description = "Alt text for file1 (WCAG 1.1.1). Optional.")
    String altText1,

    @Schema(description = "Alt text for file2 (WCAG 1.1.1). Optional.")
    String altText2
) {}
