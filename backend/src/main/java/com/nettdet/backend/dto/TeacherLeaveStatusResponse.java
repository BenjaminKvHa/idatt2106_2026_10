package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the authenticated teacher can leave a classroom.")
public record TeacherLeaveStatusResponse(
    @Schema(description = "True when at least one other teacher remains in the classroom.", example = "true")
    boolean canLeave
) {
}
