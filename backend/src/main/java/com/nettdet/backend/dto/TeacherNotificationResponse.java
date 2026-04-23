package com.nettdet.backend.dto;

import java.time.LocalDateTime;

import com.nettdet.backend.domain.enums.NotificationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher-facing classroom notification. The status is the value the notification had when the list was fetched.")
public record TeacherNotificationResponse(
    @Schema(description = "Unique notification id.", example = "1")
    Long id,
    @Schema(description = "Classroom code the notification belongs to. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Legal name of the pupil the notification is about.", example = "Pupil Name")
    String pupilLegalName,
    @Schema(description = "Human-readable notification text.", example = "Pupil requested help")
    String text,
    @Schema(description = "Time the notification was created.", example = "2026-04-23T10:15:30")
    LocalDateTime createdAt,
    @Schema(description = "Original notification read status at fetch time.", example = "UNREAD")
    NotificationStatus statusNotifications
) {
}
