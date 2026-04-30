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

/**
 * Application service for the teacher-facing notification feed.
 * <p>
 * Notifications are produced when something happens in a classroom that a teacher should
 * be aware of (for example, a pupil submitting a Weekly Mystery). Each row is tied both to
 * the {@link Classroom} and to the originating {@link PupilClassroomMembership}.
 * <p>
 * Two consumer flows are supported:
 * <ul>
 *     <li><b>Inbox view</b> – {@link #getTeacherNotifications(String, JwtUserPrincipal)} returns
 *         the classroom's full notification history newest-first and, as a side effect, marks any
 *         {@code UNREAD} entries it returns as {@code READ}. The returned DTOs reflect the
 *         status the entries had <em>before</em> being flipped, so the UI can highlight them as
 *         freshly-arrived.</li>
 *     <li><b>Badge check</b> – {@link #hasUnreadNotifications(String, JwtUserPrincipal)} performs
 *         a read-only existence query for {@code UNREAD} entries, intended for "do I have a red
 *         dot?" polling.</li>
 * </ul>
 * Notification creation is invoked from other services (such as
 * {@link WeeklyMysteryService}) and is not exposed as a public endpoint.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ClassroomRepository classroomRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    /**
     * Constructs a {@code NotificationService} with the repositories needed to persist
     * notifications and to authorise teacher-side reads.
     *
     * @param notificationRepository  repository for the notification table itself
     * @param classroomRepository     repository used to validate classroom existence
     * @param membershipRepository    repository used to resolve the originating pupil membership
     * @param teacherBridgeRepository repository used to verify teacher↔classroom ownership
     */
    public NotificationService(NotificationRepository notificationRepository,
                               ClassroomRepository classroomRepository,
                               PupilClassroomMembershipRepository membershipRepository,
                               AccountClassroomBridgeTeacherRepository teacherBridgeRepository) {
        this.notificationRepository = notificationRepository;
        this.classroomRepository = classroomRepository;
        this.membershipRepository = membershipRepository;
        this.teacherBridgeRepository = teacherBridgeRepository;
    }

    /**
     * Persists a new {@link NotificationStatus#UNREAD} notification for the given classroom,
     * tagged with the originating pupil membership.
     * <p>
     * Inputs are trimmed; empty values are rejected. Used by other services to surface
     * pupil-driven events to the classroom's teachers.
     *
     * @param classroomCode the classroom this notification belongs to
     * @param pupilEmail    the email of the pupil that triggered the event
     * @param text          the human-readable notification body (must not be blank)
     * @return the persisted {@link Notification}
     * @throws IllegalArgumentException if any input is null or blank
     * @throws EntityNotFoundException  if the classroom or pupil membership does not exist
     */
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

    /**
     * Returns the classroom's full notification history (newest first) for the requesting
     * teacher and, as a side effect, marks every {@code UNREAD} entry in the result as
     * {@code READ}.
     * <p>
     * The returned DTOs carry the status the rows had <em>before</em> being flipped, so the
     * frontend can render newly-arrived notifications differently on this page load.
     *
     * @param classroomCode the classroom whose notifications are being read
     * @param principal     the authenticated teacher; must hold the {@code TEACHER} role and be
     *                      linked to {@code classroomCode}
     * @return the classroom's notifications, newest first
     * @throws AccessDeniedException   if the principal is not a teacher of the classroom
     * @throws EntityNotFoundException if the classroom does not exist
     */
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

    /**
     * Reports whether the requesting teacher's classroom has any {@code UNREAD} notifications,
     * without mutating any row.
     * <p>
     * Intended for lightweight polling (e.g. a "you have unread notifications" badge).
     *
     * @param classroomCode the classroom to check
     * @param principal     the authenticated teacher; must hold the {@code TEACHER} role and be
     *                      linked to {@code classroomCode}
     * @return a response carrying the classroom code and the unread flag
     * @throws AccessDeniedException   if the principal is not a teacher of the classroom
     * @throws EntityNotFoundException if the classroom does not exist
     */
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
