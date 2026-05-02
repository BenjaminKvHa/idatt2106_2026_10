package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Small response used by teacher UIs to show an unread-notification count.")
public record UnreadNotificationsResponse(
    @Schema(description = "Classroom code that was checked. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Number of unread notifications currently in the classroom.", example = "3")
    long unreadNotificationsCount
) {
}
