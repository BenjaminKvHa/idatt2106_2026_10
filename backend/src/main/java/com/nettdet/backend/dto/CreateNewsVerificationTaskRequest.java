package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.TaskDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new Nyhetskvartalet (News Verification) task.")
public record CreateNewsVerificationTaskRequest(
    @Schema(description = "Chapter ID to associate the task with.", example = "2")
    Long chapterId,

    @Schema(description = "Difficulty level for the task.", example = "MEDIUM")
    TaskDifficulty difficulty,

    @Schema(description = "Task description shown to the pupil.", example = "Velg hvilken av sakene som er troverdig.")
    String taskDescription,

    @Schema(description = "First candidate file object ID.", example = "101")
    Long fileId_1,

    @Schema(description = "Second candidate file object ID.", example = "102")
    Long fileId_2,

    @Schema(description = "Correct file object ID (must match either fileId_1 or fileId_2).", example = "101")
    Long correctFileId,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String incorrectDescription
) {}
