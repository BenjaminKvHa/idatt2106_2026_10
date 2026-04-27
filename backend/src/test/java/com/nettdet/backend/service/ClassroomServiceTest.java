package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.AcceptedClassroomPupilResponse;
import com.nettdet.backend.dto.AcceptedPupilMembershipResponse;
import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.InviteTeacherRequest;
import com.nettdet.backend.dto.TeacherLeaveStatusResponse;
import com.nettdet.backend.dto.UpdateClassroomRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.util.ClassroomCodeGenerator;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    private static final String CLASSROOM_CODE = "ABCABC";
    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final String PUPIL_EMAIL = "pupil@example.com";

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AccountClassroomBridgeTeacherRepository bridgeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PupilClassroomMembershipRepository pupilClassroomMembershipRepository;

    @Mock
    private ClassroomCodeGenerator classroomCodeGenerator;

    @Spy
    private PupilXpService pupilXpService = new PupilXpService();

    @InjectMocks
    private ClassroomService classroomService;

    @DisplayName("Creates classroom with normalized fields and bridges the authenticated teacher")
    @Test
    void createClassroom_whenTeacherIsAuthenticated_createsClassroomAndTeacherBridge() {
        Account teacher = account(TEACHER_EMAIL, "Teacher Name", AccountRole.TEACHER);
        Classroom savedClassroom = classroom(CLASSROOM_CODE, "8A", "Mandag");
        when(accountRepository.findById(TEACHER_EMAIL)).thenReturn(Optional.of(teacher));
        when(classroomCodeGenerator.generateCode()).thenReturn(CLASSROOM_CODE);
        when(classroomRepository.existsById(CLASSROOM_CODE)).thenReturn(false);
        when(classroomRepository.saveAndFlush(any(Classroom.class))).thenReturn(savedClassroom);

        ClassroomResponse response = classroomService.createClassroom(
                new CreateClassroomRequest("  8A  ", "  Mandag  "),
                teacherPrincipal());

        ArgumentCaptor<Classroom> classroomCaptor = ArgumentCaptor.forClass(Classroom.class);
        ArgumentCaptor<AccountClassroomBridgeTeacher> bridgeCaptor =
                ArgumentCaptor.forClass(AccountClassroomBridgeTeacher.class);
        verify(classroomRepository).saveAndFlush(classroomCaptor.capture());
        verify(bridgeRepository).save(bridgeCaptor.capture());
        assertEquals("8A", classroomCaptor.getValue().getClassroomName());
        assertEquals("Mandag", classroomCaptor.getValue().getClassroomDescription());
        assertEquals(TEACHER_EMAIL, bridgeCaptor.getValue().getId().getAccountEmail());
        assertEquals(CLASSROOM_CODE, bridgeCaptor.getValue().getId().getClassroomCode());
        assertEquals(CLASSROOM_CODE, response.classroomCode());
    }

    @DisplayName("Retries generated classroom codes that already exist")
    @Test
    void createClassroom_whenGeneratedCodeAlreadyExists_retriesUntilUniqueCodeIsSaved() {
        Account teacher = account(TEACHER_EMAIL, "Teacher Name", AccountRole.TEACHER);
        Classroom savedClassroom = classroom("UNIQUE", "8A", null);
        when(accountRepository.findById(TEACHER_EMAIL)).thenReturn(Optional.of(teacher));
        when(classroomCodeGenerator.generateCode()).thenReturn("EXISTS", "UNIQUE");
        when(classroomRepository.existsById("EXISTS")).thenReturn(true);
        when(classroomRepository.existsById("UNIQUE")).thenReturn(false);
        when(classroomRepository.saveAndFlush(any(Classroom.class))).thenReturn(savedClassroom);

        ClassroomResponse response = classroomService.createClassroom(
                new CreateClassroomRequest("8A", ""),
                teacherPrincipal());

        assertEquals("UNIQUE", response.classroomCode());
        assertNull(response.classroomDescription());
        verify(classroomRepository, never()).saveAndFlush(argThatClassroomCode("EXISTS"));
    }

    @DisplayName("Denies classroom creation for non-teacher principals")
    @Test
    void createClassroom_whenPrincipalIsNotTeacher_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, AccountRole.PUPIL.name());

        assertThrows(
                AccessDeniedException.class,
                () -> classroomService.createClassroom(new CreateClassroomRequest("8A", null), principal));

        verifyNoInteractions(accountRepository, classroomRepository, bridgeRepository);
    }

    @DisplayName("Returns true when a pupil validates an existing classroom code")
    @Test
    void validateClassroomCode_whenCodeExists_returnsTrue() {
        when(classroomRepository.existsById(CLASSROOM_CODE)).thenReturn(true);

        ValidateClassroomCodeResponse response = classroomService.validateClassroomCode(
                new ValidateClassroomCodeRequest(" abcabc "),
                pupilPrincipal());

        assertTrue(response.exists());
        verify(classroomRepository).existsById(CLASSROOM_CODE);
    }

    @DisplayName("Returns false when a pupil validates a missing classroom code")
    @Test
    void validateClassroomCode_whenCodeDoesNotExist_returnsFalse() {
        when(classroomRepository.existsById("MISSING")).thenReturn(false);

        ValidateClassroomCodeResponse response = classroomService.validateClassroomCode(
                new ValidateClassroomCodeRequest("MISSING"),
                pupilPrincipal());

        assertFalse(response.exists());
    }

    @DisplayName("Returns false for blank classroom codes without querying the database")
    @Test
    void validateClassroomCode_whenCodeIsBlank_returnsFalse() {
        ValidateClassroomCodeResponse response = classroomService.validateClassroomCode(
                new ValidateClassroomCodeRequest("   "),
                pupilPrincipal());

        assertFalse(response.exists());
        verify(classroomRepository, never()).existsById(any());
    }

    @DisplayName("Denies classroom code validation for non-pupil principals")
    @Test
    void validateClassroomCode_whenPrincipalIsNotPupil_throwsAccessDenied() {
        assertThrows(
                AccessDeniedException.class,
                () -> classroomService.validateClassroomCode(
                        new ValidateClassroomCodeRequest(CLASSROOM_CODE),
                        teacherPrincipal()));

        verifyNoInteractions(classroomRepository);
    }

    @DisplayName("Invites only existing teacher accounts to classrooms the authenticated teacher owns")
    @Test
    void inviteTeacher_whenInvitedAccountIsTeacher_savesTeacherBridge() {
        Classroom classroom = classroom(CLASSROOM_CODE, "8A", null);
        Account invitedTeacher = account("other.teacher@example.com", "Other Teacher", AccountRole.TEACHER);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(accountRepository.findById("other.teacher@example.com")).thenReturn(Optional.of(invitedTeacher));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode("other.teacher@example.com", CLASSROOM_CODE))
                .thenReturn(false);

        classroomService.inviteTeacher(
                CLASSROOM_CODE,
                new InviteTeacherRequest("  other.teacher@example.com  "),
                teacherPrincipal());

        ArgumentCaptor<AccountClassroomBridgeTeacher> bridgeCaptor =
                ArgumentCaptor.forClass(AccountClassroomBridgeTeacher.class);
        verify(bridgeRepository).save(bridgeCaptor.capture());
        assertEquals("other.teacher@example.com", bridgeCaptor.getValue().getId().getAccountEmail());
        assertEquals(CLASSROOM_CODE, bridgeCaptor.getValue().getId().getClassroomCode());
    }

    @DisplayName("Rejects inviting pupil accounts as classroom teachers")
    @Test
    void inviteTeacher_whenInvitedAccountIsPupil_throwsIllegalArgumentException() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(accountRepository.findById(PUPIL_EMAIL)).thenReturn(Optional.of(account(
                PUPIL_EMAIL,
                "Pupil Name",
                AccountRole.PUPIL)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> classroomService.inviteTeacher(
                        CLASSROOM_CODE,
                        new InviteTeacherRequest(PUPIL_EMAIL),
                        teacherPrincipal()));

        assertEquals("Account must have teacher role", exception.getMessage());
        verify(bridgeRepository, never()).save(any(AccountClassroomBridgeTeacher.class));
    }

    @DisplayName("Updates classroom only when authenticated teacher belongs to it")
    @Test
    void updateClassroom_whenTeacherOwnsClassroom_savesNormalizedClassroom() {
        Classroom classroom = classroom(CLASSROOM_CODE, "Old", "Old description");
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(classroomRepository.save(classroom)).thenReturn(classroom);

        ClassroomResponse response = classroomService.updateClassroom(
                CLASSROOM_CODE,
                new UpdateClassroomRequest("  New name  ", "   "),
                teacherPrincipal());

        assertEquals("New name", response.classroomName());
        assertNull(response.classroomDescription());
        verify(classroomRepository).save(classroom);
    }

    @DisplayName("Prevents the only teacher from leaving a classroom")
    @Test
    void leaveClassroom_whenOnlyOneTeacherRemains_throwsIllegalArgumentException() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(bridgeRepository.countByIdClassroomCode(CLASSROOM_CODE)).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> classroomService.leaveClassroom(CLASSROOM_CODE, teacherPrincipal()));

        assertEquals("Cannot leave classroom without another teacher remaining", exception.getMessage());
        verify(bridgeRepository, never()).deleteByIdAccountEmailAndIdClassroomCode(any(), any());
    }

    @DisplayName("Allows teacher to leave classroom when another teacher remains")
    @Test
    void leaveClassroom_whenAnotherTeacherRemains_deletesTeacherBridge() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(bridgeRepository.countByIdClassroomCode(CLASSROOM_CODE)).thenReturn(2L);

        classroomService.leaveClassroom(CLASSROOM_CODE, teacherPrincipal());

        verify(bridgeRepository).deleteByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE);
    }

    @DisplayName("Returns whether the authenticated teacher can leave a classroom")
    @Test
    void getTeacherLeaveStatus_whenTeacherOwnsClassroom_returnsCanLeaveState() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(bridgeRepository.countByIdClassroomCode(CLASSROOM_CODE)).thenReturn(2L);

        TeacherLeaveStatusResponse response = classroomService.getTeacherLeaveStatus(CLASSROOM_CODE, teacherPrincipal());

        assertTrue(response.canLeave());
    }

    @DisplayName("Returns accepted pupils sorted by username with avatar object keys and XP")
    @Test
    void getAcceptedPupils_whenTeacherOwnsClassroom_returnsSortedAcceptedPupils() {
        Classroom classroom = classroom(CLASSROOM_CODE, "8A", null);
        PupilClassroomMembership bob = membership("bob@example.com", "Bob", "zulu", classroom, null);
        bob.setChapterProgressions(List.of(progression(4, 0)));
        PupilClassroomMembership alice = membership(
                "alice@example.com",
                "Alice",
                "alpha",
                classroom,
                avatar("avatar-images/alice.png"));
        alice.setChapterProgressions(List.of(progression(3, 1)));
        PupilClassroomMembership noUsername = membership("no-username@example.com", "No Username", null, classroom, null);
        noUsername.setChapterProgressions(List.of(progression(null, null)));
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(pupilClassroomMembershipRepository
                .findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(CLASSROOM_CODE))
                .thenReturn(List.of(bob, noUsername, alice));

        List<AcceptedClassroomPupilResponse> response = classroomService.getAcceptedPupils(
                CLASSROOM_CODE,
                teacherPrincipal());

        assertEquals(List.of("alice@example.com", "bob@example.com", "no-username@example.com"), response.stream()
                .map(AcceptedClassroomPupilResponse::accountEmail)
                .toList());
        assertEquals(List.of("Alice", "Bob", "No Username"), response.stream()
                .map(AcceptedClassroomPupilResponse::legalName)
                .toList());
        assertEquals(List.of(120L, 120L, 0L), response.stream()
                .map(AcceptedClassroomPupilResponse::totalXp)
                .toList());
        assertEquals("avatar-images/alice.png", response.getFirst().avatarObjectKey());
        assertNull(response.get(1).avatarObjectKey());
    }

    @DisplayName("Updates confirmed pupil username for classrooms the teacher owns")
    @Test
    void updatePupilUsername_whenTeacherOwnsClassroomAndPupilIsConfirmed_savesTrimmedUsername() {
        Classroom classroom = classroom(CLASSROOM_CODE, "8A", null);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", "oldname", classroom, null);
        membership.setChapterProgressions(List.of(progression(3, 1)));
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(pupilClassroomMembershipRepository.save(membership)).thenReturn(membership);

        AcceptedClassroomPupilResponse response = classroomService.updatePupilUsername(
                CLASSROOM_CODE,
                "  pupil@example.com  ",
                new com.nettdet.backend.dto.UpdatePupilUsernameRequest("  nydetektiv  "),
                teacherPrincipal());

        assertEquals("nydetektiv", membership.getUsername());
        assertEquals("nydetektiv", response.username());
        assertEquals(120L, response.totalXp());
        verify(pupilClassroomMembershipRepository).save(membership);
    }

    @DisplayName("Rejects blank pupil username updates")
    @Test
    void updatePupilUsername_whenUsernameIsBlank_throwsIllegalArgumentException() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership(PUPIL_EMAIL, "Pupil Name", "oldname", classroom(CLASSROOM_CODE, "8A", null), null)));

        assertThrows(IllegalArgumentException.class, () -> classroomService.updatePupilUsername(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                new com.nettdet.backend.dto.UpdatePupilUsernameRequest("   "),
                teacherPrincipal()));

        verify(pupilClassroomMembershipRepository, never()).save(any());
    }

    @DisplayName("Denies pupil username updates for teachers outside the classroom")
    @Test
    void updatePupilUsername_whenTeacherDoesNotOwnClassroom_throwsAccessDenied() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> classroomService.updatePupilUsername(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                new com.nettdet.backend.dto.UpdatePupilUsernameRequest("nydetektiv"),
                teacherPrincipal()));

        verify(pupilClassroomMembershipRepository, never())
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(any(), any());
    }

    @DisplayName("Accepts only pending pupil requests for classrooms the teacher owns")
    @Test
    void acceptPupilRequest_whenPendingMembershipExists_marksMembershipAsConfirmed() {
        Classroom classroom = classroom(CLASSROOM_CODE, "8A", null);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", "detektiv", classroom, null);
        membership.setMembershipConfirmedByTeacher(false);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(pupilClassroomMembershipRepository.save(membership)).thenReturn(membership);

        AcceptedPupilMembershipResponse response = classroomService.acceptPupilRequest(
                CLASSROOM_CODE,
                "  pupil@example.com  ",
                teacherPrincipal());

        assertTrue(membership.isMembershipConfirmedByTeacher());
        assertEquals(PUPIL_EMAIL, response.accountEmail());
        assertTrue(response.membershipConfirmedByTeacher());
    }

    @DisplayName("Allows pupils to leave only their own classroom membership")
    @Test
    void leaveClassroomAsPupil_whenMembershipExists_deletesMembership() {
        Classroom classroom = classroom(CLASSROOM_CODE, "8A", null);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, "Pupil Name", "detektiv", classroom, null);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(pupilClassroomMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));

        classroomService.leaveClassroomAsPupil(CLASSROOM_CODE, pupilPrincipal());

        verify(pupilClassroomMembershipRepository).delete(membership);
    }

    @DisplayName("Returns not found when a pupil tries to leave a classroom they are not in")
    @Test
    void leaveClassroomAsPupil_whenMembershipIsMissing_throwsEntityNotFoundException() {
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE, "8A", null)));
        when(pupilClassroomMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> classroomService.leaveClassroomAsPupil(CLASSROOM_CODE, pupilPrincipal()));

        assertEquals("Pupil membership not found", exception.getMessage());
    }

    private Classroom argThatClassroomCode(String classroomCode) {
        return org.mockito.ArgumentMatchers.argThat(classroom -> classroomCode.equals(classroom.getClassroomCode()));
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", TEACHER_EMAIL, AccountRole.TEACHER.name());
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, AccountRole.PUPIL.name());
    }

    private Account account(String email, String legalName, AccountRole role) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(legalName);
        account.setAccountRole(role);
        account.setPasswordData("encoded-password");
        return account;
    }

    private Classroom classroom(String classroomCode, String name, String description) {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(classroomCode);
        classroom.setClassroomName(name);
        classroom.setClassroomDescription(description);
        return classroom;
    }

    private PupilClassroomMembership membership(String email,
                                                String legalName,
                                                String username,
                                                Classroom classroom,
                                                FileObject avatarFileObject) {
        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(email);
        membership.setAccount(account(email, legalName, AccountRole.PUPIL));
        membership.setClassroom(classroom);
        membership.setUsername(username);
        membership.setMembershipConfirmedByTeacher(true);
        membership.setAvatarFileObject(avatarFileObject);
        return membership;
    }

    private FileObject avatar(String objectKey) {
        FileObject fileObject = new FileObject();
        fileObject.setObjectKey(objectKey);
        fileObject.setFileName(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        return fileObject;
    }

    private PupilChapterBridgeProgression progression(Integer highscore, Integer successfulRuns) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setHighscore(highscore);
        progression.setSuccessfulRuns(successfulRuns);
        return progression;
    }
}
