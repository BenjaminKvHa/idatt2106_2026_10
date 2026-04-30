package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.Notification;
import com.nettdet.backend.domain.enums.NotificationStatus;

/**
 * Repository interface for managing {@link Notification} entities.
 * 
 * <p>Handles persistence for teacher-directed alerts and provides optimized methods
 * to retrieve notifications within a classroom context, maintaining chronological order.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Retrieves all notifications for a specific classroom, ordered by creation date descending.
     * Uses an {@link EntityGraph} to eagerly load associated classroom and pupil account details,
     * reducing the number of database roundtrips.
     *
     * @param classroomCode the unique code of the classroom
     * @return a sorted list of notifications with pre-loaded associations
     */
    @EntityGraph(attributePaths = {"classroom", "pupilClassroomMembership.account"})
    List<Notification> findAllByClassroomClassroomCodeOrderByCreatedAtDesc(String classroomCode);

    /**
     * Checks if there are any notifications in a classroom that match a specific status.
     * Typically used to detect if unread alerts exist for a teacher.
     *
     * @param classroomCode       the unique code of the classroom
     * @param statusNotifications the {@link NotificationStatus} to check for
     * @return true if at least one matching notification exists, false otherwise
     */
    boolean existsByClassroomClassroomCodeAndStatusNotifications(
            String classroomCode,
            NotificationStatus statusNotifications);
}
