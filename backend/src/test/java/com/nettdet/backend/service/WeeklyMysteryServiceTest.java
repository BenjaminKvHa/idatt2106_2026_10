package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.WeeklyMystery;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.dto.CreateWeeklyMysteryResponse;
import com.nettdet.backend.dto.PupilWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherApprovedWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherPendingWeeklyMysteryResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.WeeklyMysteryRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class WeeklyMysteryServiceTest {

    private static final String CLASSROOM_CODE = "ABCABC";
    private static final String PUPIL_EMAIL = "pupil@example.com";
    private static final String TEACHER_EMAIL = "teacher@example.com";

    @Mock
    private WeeklyMysteryRepository weeklyMysteryRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private FileObjectService fileObjectService;

    @Mock
    private FileObjectAccessControlService fileObjectAccessControlService;

    @Mock
    private NotificationService notificationService;

    private WeeklyMysteryService weeklyMysteryService;

    @BeforeEach
    void setUp() {
        weeklyMysteryService = new WeeklyMysteryService(
                weeklyMysteryRepository,
                classroomRepository,
                membershipRepository,
                teacherBridgeRepository,
                fileObjectRepository,
                fileObjectService,
                fileObjectAccessControlService,
                notificationService);
    }

    @DisplayName("Creates pending weekly mystery for confirmed pupil and teacher-only file")
    @Test
    void submitMystery_whenPupilIsConfirmed_createsPendingMystery() {
        Classroom classroom = classroom();
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", classroom);
        FileObject fileObject = fileObject(10L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom);
        MockMultipartFile file = new MockMultipartFile("file", "mystery.png", "image/png", new byte[] {1});
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                PUPIL_EMAIL,
                CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(fileObjectAccessControlService.uploadWeeklyMysteryFile(file, CLASSROOM_CODE, pupilPrincipal()))
                .thenReturn(fileObject);
        when(weeklyMysteryRepository.saveAndFlush(any(WeeklyMystery.class))).thenAnswer(invocation -> {
            WeeklyMystery mystery = invocation.getArgument(0);
            mystery.setId(1L);
            ReflectionTestUtils.setField(mystery, "createdAt", LocalDateTime.of(2026, 4, 23, 10, 15));
            return mystery;
        });

        CreateWeeklyMysteryResponse response = weeklyMysteryService.submitMystery(
                " ABCABC ",
                file,
                "  Min kommentar  ",
                "  Min alt-tekst  ",
                pupilPrincipal());

        ArgumentCaptor<WeeklyMystery> mysteryCaptor = ArgumentCaptor.forClass(WeeklyMystery.class);
        verify(weeklyMysteryRepository).saveAndFlush(mysteryCaptor.capture());
        WeeklyMystery captured = mysteryCaptor.getValue();
        assertEquals(classroom, captured.getClassroom());
        assertEquals(membership, captured.getPupilClassroomMembership());
        assertEquals(fileObject, captured.getFileObject());
        assertEquals("Min kommentar", captured.getComment());
        assertEquals("Min alt-tekst", captured.getAltText());
        assertFalse(captured.isApproved());
        assertNull(captured.getApprovedAt());
        assertEquals(1L, response.id());
        assertEquals(10L, response.fileObjectId());
        assertEquals("Min kommentar", response.comment());
        assertEquals("Min alt-tekst", response.altText());
        assertFalse(response.approved());
        assertNull(response.approvedAt());
        verify(notificationService).createNotification(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                "Pupil Name har sendt inn et nytt bidrag til Ukens mysterium.");
    }

    @DisplayName("Allows null altText for pupil submission")
    @Test
    void submitMystery_whenPupilIsConfirmedAndAltTextIsNull_createsPendingMysteryWithNullAltText() {
        Classroom classroom = classroom();
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", classroom);
        FileObject fileObject = fileObject(10L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom);
        MockMultipartFile file = new MockMultipartFile("file", "mystery.png", "image/png", new byte[] {1});
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                PUPIL_EMAIL,
                CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(fileObjectAccessControlService.uploadWeeklyMysteryFile(file, CLASSROOM_CODE, pupilPrincipal()))
                .thenReturn(fileObject);
        when(weeklyMysteryRepository.saveAndFlush(any(WeeklyMystery.class))).thenAnswer(invocation -> {
            WeeklyMystery mystery = invocation.getArgument(0);
            mystery.setId(1L);
            ReflectionTestUtils.setField(mystery, "createdAt", LocalDateTime.of(2026, 4, 23, 10, 15));
            return mystery;
        });

        CreateWeeklyMysteryResponse response = weeklyMysteryService.submitMystery(
                CLASSROOM_CODE,
                file,
                "My comment",
                null,
                pupilPrincipal());

        ArgumentCaptor<WeeklyMystery> mysteryCaptor = ArgumentCaptor.forClass(WeeklyMystery.class);
        verify(weeklyMysteryRepository).saveAndFlush(mysteryCaptor.capture());
        WeeklyMystery captured = mysteryCaptor.getValue();
        assertNull(captured.getAltText());
        assertNull(response.altText());
    }

    @DisplayName("Rejects weekly mystery submission from unconfirmed pupil")
    @Test
    void submitMystery_whenPupilIsNotConfirmed_deniesSubmission() {
        MockMultipartFile file = new MockMultipartFile("file", "mystery.png", "image/png", new byte[] {1});
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                PUPIL_EMAIL,
                CLASSROOM_CODE))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> weeklyMysteryService.submitMystery(
                CLASSROOM_CODE,
                file,
                "Comment",
                "Alt text",
                pupilPrincipal()));

        verifyNoInteractions(fileObjectAccessControlService, weeklyMysteryRepository, notificationService);
    }

    @DisplayName("Returns only approved mysteries for confirmed pupils")
    @Test
    void getApprovedMysteries_whenPupilIsConfirmed_returnsApprovedMysteries() {
        Classroom classroom = classroom();
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", classroom);
        WeeklyMystery approved = weeklyMystery(
                2L,
                fileObject(20L, ClassroomAccess.ONLY_CLASS, classroom),
                classroom,
                membership,
                "Approved mystery",
                true);
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                PUPIL_EMAIL,
                CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 24, 9, 30);
        approved.setApprovedAt(approvedAt);
        when(weeklyMysteryRepository.findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc(CLASSROOM_CODE))
                .thenReturn(List.of(approved));

        List<PupilWeeklyMysteryResponse> response = weeklyMysteryService.getApprovedMysteries(
                CLASSROOM_CODE,
                pupilPrincipal());

        assertEquals(1, response.size());
        assertEquals(2L, response.getFirst().id());
        assertEquals(20L, response.getFirst().fileObjectId());
        assertEquals("Approved mystery", response.getFirst().comment());
        assertEquals(approvedAt, response.getFirst().approvedAt());
    }

    @DisplayName("Returns pending mysteries only for teachers in the classroom")
    @Test
    void getPendingMysteries_whenTeacherHasAccess_returnsPendingMysteries() {
        Classroom classroom = classroom();
        WeeklyMystery pending = weeklyMystery(
                3L,
                fileObject(30L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom),
                classroom,
                membership(PUPIL_EMAIL, "Pupil Name", classroom),
                "Pending mystery",
                false);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc(CLASSROOM_CODE))
                .thenReturn(List.of(pending));

        List<TeacherPendingWeeklyMysteryResponse> response = weeklyMysteryService.getPendingMysteries(
                CLASSROOM_CODE,
                teacherPrincipal());

        assertEquals(1, response.size());
        assertEquals(3L, response.getFirst().id());
        assertEquals(30L, response.getFirst().fileObjectId());
        assertEquals(PUPIL_EMAIL, response.getFirst().pupilEmail());
        assertEquals("Pupil Name", response.getFirst().pupilLegalName());
    }

    @DisplayName("Denies pending mystery listing for teachers outside the classroom")
    @Test
    void getPendingMysteries_whenTeacherDoesNotBelongToClassroom_deniesAccess() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom()));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> weeklyMysteryService.getPendingMysteries(
                CLASSROOM_CODE,
                teacherPrincipal()));

        verify(weeklyMysteryRepository, org.mockito.Mockito.never())
                .findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc(CLASSROOM_CODE);
    }

    @DisplayName("Returns approved mysteries with pupil legal name for teachers in the classroom")
    @Test
    void getApprovedMysteriesForTeacher_whenTeacherHasAccess_returnsApprovedMysteriesWithPupilName() {
        Classroom classroom = classroom();
        WeeklyMystery approved = weeklyMystery(
                6L,
                fileObject(60L, ClassroomAccess.ONLY_CLASS, classroom),
                classroom,
                membership(PUPIL_EMAIL, "Pupil Name", classroom),
                "Approved mystery",
                true);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 24, 9, 30);
        approved.setApprovedAt(approvedAt);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc(CLASSROOM_CODE))
                .thenReturn(List.of(approved));

        List<TeacherApprovedWeeklyMysteryResponse> response = weeklyMysteryService.getApprovedMysteriesForTeacher(
                CLASSROOM_CODE,
                teacherPrincipal());

        assertEquals(1, response.size());
        assertEquals(6L, response.getFirst().id());
        assertEquals(60L, response.getFirst().fileObjectId());
        assertEquals("Pupil Name", response.getFirst().pupilLegalName());
        assertEquals("Approved mystery", response.getFirst().comment());
        assertEquals(approvedAt, response.getFirst().approvedAt());
    }

    @DisplayName("Denies approved mystery listing for teachers outside the classroom")
    @Test
    void getApprovedMysteriesForTeacher_whenTeacherDoesNotBelongToClassroom_deniesAccess() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom()));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> weeklyMysteryService.getApprovedMysteriesForTeacher(
                CLASSROOM_CODE,
                teacherPrincipal()));

        verify(weeklyMysteryRepository, org.mockito.Mockito.never())
                .findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc(CLASSROOM_CODE);
    }

    @DisplayName("Approves mystery and changes linked file access to classroom-only")
    @Test
    void acceptMystery_whenTeacherHasAccess_approvesMysteryAndUpdatesFileAccess() {
        Classroom classroom = classroom();
        FileObject fileObject = fileObject(40L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom);
        WeeklyMystery pending = weeklyMystery(
                4L,
                fileObject,
                classroom,
                membership(PUPIL_EMAIL, "Pupil Name", classroom),
                "Pending mystery",
                false);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findByIdAndClassroomClassroomCode(4L, CLASSROOM_CODE))
                .thenReturn(Optional.of(pending));
        when(weeklyMysteryRepository.save(pending)).thenReturn(pending);

        PupilWeeklyMysteryResponse response = weeklyMysteryService.acceptMystery(
                CLASSROOM_CODE,
                4L,
                null,
                teacherPrincipal());

        assertTrue(pending.isApproved());
        assertNotNull(pending.getApprovedAt());
        assertEquals(ClassroomAccess.ONLY_CLASS, fileObject.getAccessLevel());
        assertEquals(4L, response.id());
        assertEquals(pending.getApprovedAt(), response.approvedAt());
        verify(fileObjectRepository).save(fileObject);
        verify(weeklyMysteryRepository).save(pending);
    }

    @DisplayName("Updates alt text when teacher accepts pending mystery with non-null alt text")
    @Test
    void acceptMystery_whenTeacherProvidesAltText_updatesAltText() {
        Classroom classroom = classroom();
        FileObject fileObject = fileObject(42L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom);
        WeeklyMystery pending = weeklyMystery(
                42L,
                fileObject,
                classroom,
                membership(PUPIL_EMAIL, "Pupil Name", classroom),
                "Pending mystery",
                false);
        pending.setAltText("Old alt text");
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findByIdAndClassroomClassroomCode(42L, CLASSROOM_CODE))
                .thenReturn(Optional.of(pending));
        when(weeklyMysteryRepository.save(pending)).thenReturn(pending);

        PupilWeeklyMysteryResponse response = weeklyMysteryService.acceptMystery(
                CLASSROOM_CODE,
                42L,
                "New teacher alt text",
                teacherPrincipal());

        assertTrue(pending.isApproved());
        assertEquals("New teacher alt text", pending.getAltText());
        assertEquals("New teacher alt text", response.altText());
    }

    @DisplayName("Keeps existing approval timestamp when an already approved mystery is accepted again")
    @Test
    void acceptMystery_whenMysteryAlreadyHasApprovedAt_preservesOriginalApprovalTimestamp() {
        Classroom classroom = classroom();
        FileObject fileObject = fileObject(41L, ClassroomAccess.ONLY_CLASS, classroom);
        WeeklyMystery approved = weeklyMystery(
                41L,
                fileObject,
                classroom,
                membership(PUPIL_EMAIL, "Pupil Name", classroom),
                "Approved mystery",
                true);
        LocalDateTime originalApprovedAt = LocalDateTime.of(2026, 4, 24, 9, 30);
        approved.setApprovedAt(originalApprovedAt);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findByIdAndClassroomClassroomCode(41L, CLASSROOM_CODE))
                .thenReturn(Optional.of(approved));
        when(weeklyMysteryRepository.save(approved)).thenReturn(approved);

        PupilWeeklyMysteryResponse response = weeklyMysteryService.acceptMystery(
                CLASSROOM_CODE,
                41L,
                null,
                teacherPrincipal());

        assertTrue(approved.isApproved());
        assertEquals(originalApprovedAt, approved.getApprovedAt());
        assertEquals(originalApprovedAt, response.approvedAt());
    }

    @DisplayName("Declines mystery by deleting request before deleting linked file")
    @Test
    void declineMystery_whenTeacherHasAccess_deletesMysteryAndFile() {
        Classroom classroom = classroom();
        FileObject fileObject = fileObject(50L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom);
        WeeklyMystery pending = weeklyMystery(
                5L,
                fileObject,
                classroom,
                membership(PUPIL_EMAIL, "Pupil Name", classroom),
                "Pending mystery",
                false);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findByIdAndClassroomClassroomCode(5L, CLASSROOM_CODE))
                .thenReturn(Optional.of(pending));

        weeklyMysteryService.declineMystery(CLASSROOM_CODE, 5L, teacherPrincipal());

        verify(weeklyMysteryRepository).delete(pending);
        verify(weeklyMysteryRepository).flush();
        verify(fileObjectService).delete(50L);
    }

    @DisplayName("Returns not found when teacher tries to approve mystery from another classroom")
    @Test
    void acceptMystery_whenMysteryDoesNotBelongToClassroom_throwsNotFound() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom()));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(weeklyMysteryRepository.findByIdAndClassroomClassroomCode(99L, CLASSROOM_CODE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> weeklyMysteryService.acceptMystery(
                CLASSROOM_CODE,
                99L,
                null,
                teacherPrincipal()));
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, AccountRole.PUPIL.name());
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", TEACHER_EMAIL, AccountRole.TEACHER.name());
    }

    private Classroom classroom() {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(CLASSROOM_CODE);
        classroom.setClassroomName("Classroom");
        return classroom;
    }

    private Account account(String email, String legalName, AccountRole role) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(legalName);
        account.setPasswordData("encoded-password");
        account.setAccountRole(role);
        return account;
    }

    private PupilClassroomMembership membership(String email, String legalName, Classroom classroom) {
        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(email);
        membership.setAccount(account(email, legalName, AccountRole.PUPIL));
        membership.setClassroom(classroom);
        membership.setUsername("detektiv");
        membership.setMembershipConfirmedByTeacher(true);
        return membership;
    }

    private FileObject fileObject(Long id, ClassroomAccess accessLevel, Classroom classroom) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey("mystery-" + id + ".png");
        fileObject.setFileName("mystery-" + id + ".png");
        fileObject.setClassroom(classroom);
        fileObject.setAccessLevel(accessLevel);
        return fileObject;
    }

    private WeeklyMystery weeklyMystery(Long id,
                                        FileObject fileObject,
                                        Classroom classroom,
                                        PupilClassroomMembership membership,
                                        String comment,
                                        boolean approved) {
        WeeklyMystery mystery = new WeeklyMystery();
        mystery.setId(id);
        mystery.setFileObject(fileObject);
        mystery.setClassroom(classroom);
        mystery.setPupilClassroomMembership(membership);
        mystery.setComment(comment);
        mystery.setAltText("Alt for " + comment);
        mystery.setApproved(approved);
        ReflectionTestUtils.setField(mystery, "createdAt", LocalDateTime.of(2026, 4, 23, 10, 15));
        return mystery;
    }
}
