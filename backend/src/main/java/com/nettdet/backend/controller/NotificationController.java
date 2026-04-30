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

/**
 * REST controller for managing classroom notifications for teachers.
 * <p>
 * This controller provides endpoints for teachers to view notifications related to their classrooms
 * and check for unread updates. Notifications are scoped per classroom, and their read state is
 * managed to ensure teachers stay informed of new events, such as pupil join requests or weekly
 * mystery submissions.
 */
@RestController
@Tag(name = "Notifications", description = "Teacher-only classroom notification endpoints. Notifications are scoped to one classroom and use shared read state for all teachers in that classroom.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Constructs a new NotificationController with the required service.
     *
     * @param notificationService the service responsible for notification logic and persistence
     */
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Retrieves all notifications for a specific classroom managed by the authenticated teacher.
     * <p>
     * Returns a list of notifications containing details such as the event type and current status.
     * As a side effect, any notifications that were in an 'UNREAD' state at the time of retrieval
     * are updated to 'READ' after the response is generated, ensuring that subsequent calls
     * reflect they have been seen.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing a list of {@link TeacherNotificationResponse}
     */
    @GetMapping("/teacher/classrooms/{classroomCode}/notifications")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List classroom notifications", description = "Returns all notifications for a classroom the authenticated teacher belongs to. The response contains each notification's original status at fetch time; after the response is built, fetched unread notifications are marked as READ for the next request.")
    public ResponseEntity<List<TeacherNotificationResponse>> getTeacherNotifications(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getTeacherNotifications(classroomCode, principal));
    }

    /**
     * Checks if there are any unread notifications for a specific classroom.
     * <p>
     * This is a lightweight endpoint designed for polling or UI notification indicators (like bells).
     * It returns a boolean status indicating the presence of unread notifications without
     * modifying their state or returning full details.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing {@link UnreadNotificationsResponse}
     */
    @GetMapping("/teacher/classrooms/{classroomCode}/notifications/unread")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Check unread classroom notifications", description = "Returns whether a classroom currently has unread notifications for the teacher notification bell. This endpoint does not fetch notification details and does not mark anything as read.")
    public ResponseEntity<UnreadNotificationsResponse> hasUnreadNotifications(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.hasUnreadNotifications(classroomCode, principal));
    }
}
