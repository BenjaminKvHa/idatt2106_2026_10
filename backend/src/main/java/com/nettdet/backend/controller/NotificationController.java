package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.TeacherNotificationResponse;
import com.nettdet.backend.dto.UnreadNotificationsResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Notifications", description = "Teacher-only classroom notification endpoints. Notifications are scoped to one classroom and use shared read state for all teachers in that classroom.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/teacher/classrooms/{classroomCode}/notifications")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List classroom notifications", description = "Returns all notifications for a classroom the authenticated teacher belongs to. The response contains each notification's original status at fetch time; after the response is built, fetched unread notifications are marked as READ for the next request.")
    public ResponseEntity<List<TeacherNotificationResponse>> getTeacherNotifications(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getTeacherNotifications(classroomCode, principal));
    }

    @GetMapping("/teacher/classrooms/{classroomCode}/notifications/unread")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Check unread classroom notifications", description = "Returns whether a classroom currently has unread notifications for the teacher notification bell. This endpoint does not fetch notification details and does not mark anything as read.")
    public ResponseEntity<UnreadNotificationsResponse> hasUnreadNotifications(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.hasUnreadNotifications(classroomCode, principal));
    }
}
