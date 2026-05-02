package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.CorrectFileSlot;
import com.nettdet.backend.domain.enums.TaskDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for creating a Nyhetskvartalet (News Verification) task. Two image files are sent as `file1` and `file2` multipart parts.")
public record CreateNewsVerificationTaskRequest(
    @Schema(description = "Chapter ID to associate the task with.", example = "2")
    Long chapterId,

    @Schema(description = "Classroom code that scopes this task.", example = "ABC123")
    String classroomCode,

    @Schema(description = "Difficulty level for the task.", example = "MEDIUM")
    TaskDifficulty difficulty,

    @Schema(description = "Task description shown to the pupil.", example = "Velg hvilken av sakene som er troverdig.")
    String taskDescription,

    @Schema(description = "Which of the two uploaded files is the credible source.", example = "FILE_1")
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
