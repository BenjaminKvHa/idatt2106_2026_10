package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.DatacenterTaskFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Answer payload for one Datasenteret task. The format field selects which inner answer is read.")
public record DatacenterAnswerRequest(
    @Schema(description = "Discriminator matching the served task's format.", example = "IMAGE_VERIFICATION")
    DatacenterTaskFormat format,
    @Schema(description = "Image-verification answer, required when format is IMAGE_VERIFICATION.", nullable = true)
    ImageVerificationRequest imageAnswer,
    @Schema(description = "News-verification answer, required when format is NEWS_VERIFICATION.", nullable = true)
    NewsVerificationRequest newsAnswer
) {}
