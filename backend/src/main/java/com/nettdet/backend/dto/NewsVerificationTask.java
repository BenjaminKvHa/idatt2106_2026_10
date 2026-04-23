package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Internal/full Nyhetskvartalet task structure including correct answer data.")
public record NewsVerificationTask(
    @Schema(description = "Task description shown to the pupil.", example = "Velg hvilken av sakene som er troverdig.")
    String taskDescription,
    @Schema(description = "First candidate file object id.", example = "101")
    Long fileId_1,
    @Schema(description = "Second candidate file object id.", example = "102")
    Long fileId_2,
    @Schema(description = "Correct file object id for the task.", example = "101")
    Long correctFileId,
    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,
    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String incorrectDescription
) {}
