package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Answer payload for one Nyhetskvartalet news-verification task.")
public record NewsVerificationRequest(
    @Schema(description = "File object id chosen by the pupil as the correct news item.", example = "101")
    Long selectedFileId
) {}
