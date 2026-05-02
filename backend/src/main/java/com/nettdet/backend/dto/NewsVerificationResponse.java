package com.nettdet.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task payload returned for one Nyhetskvartalet news-verification task.")
public record NewsVerificationResponse(
    @Schema(description = "Task id used when validating the answer.", example = "23")
    Long taskId,
    @Schema(description = "Task description shown to the pupil.", example = "Velg hvilken av sakene som er troverdig.")
    String taskDescription,
    @Schema(description = "Candidate file object ids shown to the pupil for selection.")
    List<Long> fileIds,
    @Schema(description = "Alt texts aligned positionally with fileIds; entries may be null when no alt text was set.")
    List<String> altTexts
) {}
