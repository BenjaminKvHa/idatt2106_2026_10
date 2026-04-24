package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request used by pupils to check whether a classroom code exists before joining.")
public record ValidateClassroomCodeRequest(
        @Schema(description = "Six-letter classroom code entered by the pupil.", example = "ABCABC")
        String classroomCode
) {
}
