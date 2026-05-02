package com.nettdet.backend.dto;

import com.nettdet.backend.domain.enums.DatacenterTaskFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Internal/full Datasenteret task structure. Wraps either an image-verification or news-verification inner task and uses the format field as the discriminator.")
public record DatacenterTask(
    @Schema(description = "Discriminator for which inner task type is populated.", example = "IMAGE_VERIFICATION")
    DatacenterTaskFormat format,
    @Schema(description = "Populated when format is IMAGE_VERIFICATION; otherwise null.", nullable = true)
    ImageVerificationTask imageTask,
    @Schema(description = "Populated when format is NEWS_VERIFICATION; otherwise null.", nullable = true)
    NewsVerificationTask newsTask
) {}
