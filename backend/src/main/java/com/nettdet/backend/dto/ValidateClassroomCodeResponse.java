package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Boolean classroom-code lookup result.")
public record ValidateClassroomCodeResponse(
        @Schema(description = "True when a classroom exists with the submitted code.", example = "true")
        boolean exists
) {
}
