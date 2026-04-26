package com.nettdet.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full Datasenteret run payload. Contains the ordered list of tasks the pupil should solve in this attempt.")
public record DatacenterRunResponse(
    @Schema(description = "Ordered list of tasks for the run. Slots 0-1 are NEWS_VERIFICATION, slots 2-4 are IMAGE_VERIFICATION.")
    List<DatacenterTaskResponse> tasks
) {}
