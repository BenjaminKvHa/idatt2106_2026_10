package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.CorrectFileSlot;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for creating a Datasenteret task in news-verification format. Two image files are sent as `file1` and `file2` multipart parts.")
public record CreateDatacenterNewsTaskRequest(
    @Schema(description = "Task description shown to the pupil.", example = "Velg hvilken av sakene som er troverdig.")
    String taskDescription,

    @Schema(description = "Which of the two uploaded files is the credible source.", example = "FILE_1")
    CorrectFileSlot correctFileSlot,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String incorrectDescription
) {}
