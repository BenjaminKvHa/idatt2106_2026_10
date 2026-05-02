package com.nettdet.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nettdet.backend.domain.Notification;
import com.nettdet.backend.domain.enums.NotificationStatus;
import com.nettdet.backend.support.PostgresDataJpaTest;

@PostgresDataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Finds classroom notifications ordered by newest first and excludes other classrooms")
    @Test
    void findAllByClassroomClassroomCodeOrderByCreatedAtDesc_whenMultipleClassroomsExist_returnsOnlyRequestedClassroomNewestFirst() {
        insertNotificationData();
        insertNotification("ABCABC", "pupil@example.com", "Older notification", "UNREAD",
                LocalDateTime.of(2026, 4, 23, 9, 0));
        insertNotification("XYZXYZ", "other@example.com", "Other classroom notification", "UNREAD",
                LocalDateTime.of(2026, 4, 23, 12, 0));
        insertNotification("ABCABC", "pupil@example.com", "Newer notification", "READ",
                LocalDateTime.of(2026, 4, 23, 11, 0));

        List<Notification> notifications = notificationRepository
                .findAllByClassroomClassroomCodeOrderByCreatedAtDesc("ABCABC");

        assertEquals(2, notifications.size());
        assertEquals(List.of("Newer notification", "Older notification"), notifications.stream()
                .map(Notification::getNotificationText)
                .toList());
        assertTrue(notifications.stream()
                .allMatch(notification -> notification.getClassroom().getClassroomCode().equals("ABCABC")));
        assertEquals("Pupil Name", notifications.get(0).getPupilClassroomMembership().getAccount().getLegalName());
    }

    @DisplayName("Counts unread notifications only for the requested classroom")
    @Test
    void countByClassroomClassroomCodeAndStatusNotifications_whenUnreadExistsInClassroom_returnsCountOnlyForThatClassroom() {
        insertNotificationData();
        insertNotification("ABCABC", "pupil@example.com", "Unread notification", "UNREAD",
                LocalDateTime.of(2026, 4, 23, 10, 0));
        insertNotification("ABCABC", "pupil@example.com", "Second unread notification", "UNREAD",
                LocalDateTime.of(2026, 4, 23, 11, 0));
        insertNotification("XYZXYZ", "other@example.com", "Read notification", "READ",
                LocalDateTime.of(2026, 4, 23, 10, 0));

        long unreadCountForRequestedClassroom = notificationRepository
                .countByClassroomClassroomCodeAndStatusNotifications("ABCABC", NotificationStatus.UNREAD);
        long unreadCountForOtherClassroom = notificationRepository
                .countByClassroomClassroomCodeAndStatusNotifications("XYZXYZ", NotificationStatus.UNREAD);

        assertEquals(2L, unreadCountForRequestedClassroom);
        assertEquals(0L, unreadCountForOtherClassroom);
    }

    @DisplayName("Persists notification status updates using the status_notifications column")
    @Test
    void save_whenNotificationStatusChanges_persistsUpdatedStatus() {
        insertNotificationData();
        insertNotification("ABCABC", "pupil@example.com", "Unread notification", "UNREAD",
                LocalDateTime.of(2026, 4, 23, 10, 0));
        Notification notification = notificationRepository
                .findAllByClassroomClassroomCodeOrderByCreatedAtDesc("ABCABC")
                .getFirst();

        notification.setStatusNotifications(NotificationStatus.READ);
        notificationRepository.saveAndFlush(notification);

        long unreadCount = notificationRepository
                .countByClassroomClassroomCodeAndStatusNotifications("ABCABC", NotificationStatus.UNREAD);
        long readCount = notificationRepository
                .countByClassroomClassroomCodeAndStatusNotifications("ABCABC", NotificationStatus.READ);
        assertEquals(0L, unreadCount);
        assertEquals(1L, readCount);
    }

    private void insertNotificationData() {
        insertClassroom("ABCABC", "Main classroom");
        insertClassroom("XYZXYZ", "Other classroom");
        insertPupilAccount("pupil@example.com", "Pupil Name");
        insertPupilAccount("other@example.com", "Other Pupil");
        insertMembership("pupil@example.com", "ABCABC", "detektiv1");
        insertMembership("other@example.com", "XYZXYZ", "detektiv2");
    }

    private void insertClassroom(String classroomCode, String classroomName) {
        jdbcTemplate.update(
                "INSERT INTO classroom (classroom_code, classroom_name, classroom_description) VALUES (?, ?, ?)",
                classroomCode,
                classroomName,
                "Notification repository test classroom");
    }

    private void insertPupilAccount(String email, String legalName) {
        jdbcTemplate.update(
                "INSERT INTO account (email, password_data, legal_name, account_role) VALUES (?, ?, ?, ?::account_role_enum)",
                email,
                "encoded-password",
                legalName,
                "PUPIL");
    }

    private void insertMembership(String email, String classroomCode, String username) {
        jdbcTemplate.update(
                "INSERT INTO pupil_classroom_membership (account_email, classroom_code, username, membership_confirmed_by_teacher) VALUES (?, ?, ?, true)",
                email,
                classroomCode,
                username);
    }

    private void insertNotification(String classroomCode,
                                    String pupilEmail,
                                    String text,
                                    String status,
                                    LocalDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO notification (classroom_code, account_email, notification_text, status_notifications, created_at) "
                        + "VALUES (?, ?, ?, ?::notification_status_enum, ?)",
                classroomCode,
                pupilEmail,
                text,
                status,
                createdAt);
    }
}
