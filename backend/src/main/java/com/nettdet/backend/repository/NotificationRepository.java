package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.Notification;
import com.nettdet.backend.domain.enums.NotificationStatus;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"classroom", "pupilClassroomMembership.account"})
    List<Notification> findAllByClassroomClassroomCodeOrderByCreatedAtDesc(String classroomCode);

    boolean existsByClassroomClassroomCodeAndStatusNotifications(
            String classroomCode,
            NotificationStatus statusNotifications);
}
