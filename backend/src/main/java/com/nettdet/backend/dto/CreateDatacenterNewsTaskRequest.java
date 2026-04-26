package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new Datasenteret task using the news-verification format.")
public record CreateDatacenterNewsTaskRequest(
    @Schema(description = "Task description shown to the pupil.", example = "Velg hvilken av sakene som er troverdig.")
    String taskDescription,

    @Schema(description = "First candidate file object ID.", example = "201")
    Long fileId_1,

    @Schema(description = "Second candidate file object ID.", example = "202")
    Long fileId_2,

    @Schema(description = "Correct file object ID (must match either fileId_1 or fileId_2).", example = "201")
    Long correctFileId,

    @Schema(description = "Feedback shown when the pupil answers correctly.")
    String correctDescription,

    @Schema(description = "Feedback shown when the pupil answers incorrectly.")
    String incorrectDescription
) {}
