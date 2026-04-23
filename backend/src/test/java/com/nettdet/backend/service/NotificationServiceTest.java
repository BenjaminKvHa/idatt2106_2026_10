package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.nettdet.backend.domain.Account;
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

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String CLASSROOM_CODE = "ABCABC";
    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final String PUPIL_EMAIL = "pupil@example.com";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    @InjectMocks
    private NotificationService notificationService;

    @DisplayName("Creates unread notification for an existing pupil membership in the requested classroom")
    @Test
    void createNotification_whenInputIsValid_savesUnreadNotificationWithNormalizedValues() {
        Classroom classroom = classroom(CLASSROOM_CODE);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", classroom);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.createNotification(
                "  ABCABC  ",
                "  pupil@example.com  ",
                "  Pupil requested help  ");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();
        assertSame(captured, notification);
        assertSame(classroom, captured.getClassroom());
        assertSame(membership, captured.getPupilClassroomMembership());
        assertEquals("Pupil requested help", captured.getNotificationText());
        assertEquals(NotificationStatus.UNREAD, captured.getStatusNotifications());
    }

    @DisplayName("Rejects blank notification text before reading classroom or membership data")
    @Test
    void createNotification_whenTextIsBlank_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.createNotification(CLASSROOM_CODE, PUPIL_EMAIL, "   "));

        assertEquals("Notification text is required", exception.getMessage());
        verifyNoInteractions(classroomRepository, membershipRepository, notificationRepository);
    }

    @DisplayName("Rejects notification creation when pupil membership is not in the requested classroom")
    @Test
    void createNotification_whenMembershipIsMissing_throwsEntityNotFoundException() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE)));
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> notificationService.createNotification(CLASSROOM_CODE, PUPIL_EMAIL, "Pupil requested help"));

        assertEquals("Pupil membership not found", exception.getMessage());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @DisplayName("Returns original notification statuses and then marks fetched unread notifications as read")
    @Test
    void getTeacherNotifications_whenTeacherHasAccess_returnsOriginalStatusesAndMarksUnreadAsRead() {
        Classroom classroom = classroom(CLASSROOM_CODE);
        Notification unread = notification(1L, classroom, "Pupil requested help", NotificationStatus.UNREAD);
        Notification alreadyRead = notification(2L, classroom, "Pupil completed chapter", NotificationStatus.READ);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(notificationRepository.findAllByClassroomClassroomCodeOrderByCreatedAtDesc(CLASSROOM_CODE))
                .thenReturn(List.of(unread, alreadyRead));

        List<TeacherNotificationResponse> response = notificationService.getTeacherNotifications(
                CLASSROOM_CODE,
                teacherPrincipal());

        assertEquals(2, response.size());
        assertEquals(NotificationStatus.UNREAD, response.get(0).statusNotifications());
        assertEquals(NotificationStatus.READ, response.get(1).statusNotifications());
        assertEquals(NotificationStatus.READ, unread.getStatusNotifications());
        assertEquals(NotificationStatus.READ, alreadyRead.getStatusNotifications());
        verify(notificationRepository).saveAll(List.of(unread));
    }

    @DisplayName("Denies notification listing for teachers outside the classroom")
    @Test
    void getTeacherNotifications_whenTeacherDoesNotOwnClassroom_throwsAccessDenied() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE)));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> notificationService.getTeacherNotifications(CLASSROOM_CODE, teacherPrincipal()));

        verify(notificationRepository, never()).findAllByClassroomClassroomCodeOrderByCreatedAtDesc(any());
    }

    @DisplayName("Denies unread checks for non-teacher principals")
    @Test
    void hasUnreadNotifications_whenPrincipalIsNotTeacher_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, AccountRole.PUPIL.name());

        assertThrows(
                AccessDeniedException.class,
                () -> notificationService.hasUnreadNotifications(CLASSROOM_CODE, principal));

        verifyNoInteractions(classroomRepository, teacherBridgeRepository, notificationRepository);
    }

    @DisplayName("Returns unread notification state for classrooms the teacher belongs to")
    @Test
    void hasUnreadNotifications_whenTeacherHasAccess_returnsRepositoryUnreadState() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE)));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(notificationRepository.existsByClassroomClassroomCodeAndStatusNotifications(
                CLASSROOM_CODE,
                NotificationStatus.UNREAD))
                .thenReturn(true);

        UnreadNotificationsResponse response = notificationService.hasUnreadNotifications(
                CLASSROOM_CODE,
                teacherPrincipal());

        assertEquals(CLASSROOM_CODE, response.classroomCode());
        assertTrue(response.hasUnreadNotifications());
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", TEACHER_EMAIL, AccountRole.TEACHER.name());
    }

    private Classroom classroom(String classroomCode) {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(classroomCode);
        classroom.setClassroomName("Test classroom");
        return classroom;
    }

    private PupilClassroomMembership membership(String email, String legalName, Classroom classroom) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(legalName);
        account.setAccountRole(AccountRole.PUPIL);
        account.setPasswordData("encoded-password");

        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(email);
        membership.setAccount(account);
        membership.setClassroom(classroom);
        membership.setUsername("detektiv");
        membership.setMembershipConfirmedByTeacher(true);
        return membership;
    }

    private Notification notification(Long id,
                                      Classroom classroom,
                                      String text,
                                      NotificationStatus status) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setClassroom(classroom);
        notification.setPupilClassroomMembership(membership(PUPIL_EMAIL, "Pupil Name", classroom));
        notification.setNotificationText(text);
        notification.setStatusNotifications(status);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 4, 23, 10, 0));
        return notification;
    }
}
