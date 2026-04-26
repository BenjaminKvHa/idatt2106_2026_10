package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.DatacenterTaskFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single Datasenteret task envelope returned to the pupil. Contains exactly one of imageTask or newsTask, identified by the format field.")
public record DatacenterTaskResponse(
    @Schema(description = "Task id used when validating the answer.", example = "55")
    Long taskId,
    @Schema(description = "Discriminator that tells the frontend which renderer to use.", example = "NEWS_VERIFICATION")
    DatacenterTaskFormat format,
    @Schema(description = "Image-verification payload, populated when format is IMAGE_VERIFICATION.", nullable = true)
    ImageVerificationResponse imageTask,
    @Schema(description = "News-verification payload, populated when format is NEWS_VERIFICATION.", nullable = true)
    NewsVerificationResponse newsTask
) {}
