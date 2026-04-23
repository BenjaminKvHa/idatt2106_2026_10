package com.nettdet.backend.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.Notification;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.enums.NotificationStatus;
import com.nettdet.backend.dto.TeacherNotificationResponse;
import com.nettdet.backend.dto.UnreadNotificationsResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.NotificationRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ClassroomRepository classroomRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               ClassroomRepository classroomRepository,
                               PupilClassroomMembershipRepository membershipRepository,
                               AccountClassroomBridgeTeacherRepository teacherBridgeRepository) {
        this.notificationRepository = notificationRepository;
        this.classroomRepository = classroomRepository;
        this.membershipRepository = membershipRepository;
        this.teacherBridgeRepository = teacherBridgeRepository;
    }

    @Transactional
    public Notification createNotification(String classroomCode, String pupilEmail, String text) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        String normalizedPupilEmail = normalizeEmail(pupilEmail);
        String normalizedText = normalizeNotificationText(text);

        Classroom classroom = getRequiredClassroom(normalizedClassroomCode);
        PupilClassroomMembership membership = membershipRepository
                .findByAccountEmailAndClassroomClassroomCode(normalizedPupilEmail, normalizedClassroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Pupil membership not found"));

        Notification notification = new Notification();
        notification.setClassroom(classroom);
        notification.setPupilClassroomMembership(membership);
        notification.setNotificationText(normalizedText);
        notification.setStatusNotifications(NotificationStatus.UNREAD);

        return notificationRepository.save(notification);
    }

    @Transactional
    public List<TeacherNotificationResponse> getTeacherNotifications(String classroomCode,
                                                                     JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        List<Notification> notifications = notificationRepository
                .findAllByClassroomClassroomCodeOrderByCreatedAtDesc(normalizedClassroomCode);

        List<TeacherNotificationResponse> response = notifications.stream()
                .map(this::toTeacherNotificationResponse)
                .toList();

        List<Notification> unreadNotifications = notifications.stream()
                .filter(notification -> notification.getStatusNotifications() == NotificationStatus.UNREAD)
                .toList();

        unreadNotifications.forEach(notification -> notification.setStatusNotifications(NotificationStatus.READ));
        if (!unreadNotifications.isEmpty()) {
            notificationRepository.saveAll(unreadNotifications);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public UnreadNotificationsResponse hasUnreadNotifications(String classroomCode,
                                                              JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        boolean hasUnreadNotifications = notificationRepository.existsByClassroomClassroomCodeAndStatusNotifications(
                normalizedClassroomCode,
                NotificationStatus.UNREAD);

        return new UnreadNotificationsResponse(normalizedClassroomCode, hasUnreadNotifications);
    }

    private void verifyTeacherAccess(JwtUserPrincipal principal, String classroomCode) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);
    }

    private void verifyTeacherPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Teacher access is required");
        }

        if (!AccountRole.TEACHER.name().equals(principal.role())) {
            throw new AccessDeniedException("Teacher access is required");
        }
    }

    private Classroom getRequiredClassroom(String classroomCode) {
        return classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));
    }

    private void verifyTeacherOwnership(String teacherEmail, String classroomCode) {
        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(teacherEmail, classroomCode)) {
            throw new AccessDeniedException("You do not have access to this classroom");
        }
    }

    private TeacherNotificationResponse toTeacherNotificationResponse(Notification notification) {
        return new TeacherNotificationResponse(
                notification.getId(),
                notification.getClassroom().getClassroomCode(),
                notification.getPupilClassroomMembership().getAccount().getLegalName(),
                notification.getNotificationText(),
                notification.getCreatedAt(),
                notification.getStatusNotifications());
    }

    private String normalizeClassroomCode(String classroomCode) {
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new IllegalArgumentException("Classroom code is required");
        }

        return classroomCode.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Pupil email is required");
        }

        return email.trim();
    }

    private String normalizeNotificationText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Notification text is required");
        }

        return text.trim();
    }
}
