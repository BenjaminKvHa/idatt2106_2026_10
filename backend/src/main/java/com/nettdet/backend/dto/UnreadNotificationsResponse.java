package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Small response used by teacher UIs to decide whether to show an unread-notification indicator.")
public record UnreadNotificationsResponse(
    @Schema(description = "Classroom code that was checked. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "True when the classroom currently has at least one unread notification.", example = "true")
    boolean hasUnreadNotifications
) {
}
