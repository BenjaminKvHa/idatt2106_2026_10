package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mockito.junit.jupiter.MockitoExtension;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.PupilClassroomMembershipResponse;
import com.nettdet.backend.dto.PupilProgressionResponse;
import com.nettdet.backend.dto.PupilRequest;
import com.nettdet.backend.dto.PupilResponse;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;

@ExtendWith(MockitoExtension.class)
class PupilServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";
    private static final String CLASSROOM_CODE = "ABCABC";

    @Mock
    private PupilClassroomMembershipRepository pupilMembershipRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private PupilService pupilService;

    @DisplayName("Returns pupil classroom information for existing membership")
    @Test
    void getPupilInformation_whenMembershipExists_returnsPupilResponse() {
        PupilClassroomMembership membership = membership(true, "detektiv1", avatar(42L));
        membership.setChapterProgressions(List.of(progression(1L, 3, 2, 0)));
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));

        PupilResponse response = pupilService.getPupilInformation(PUPIL_EMAIL, CLASSROOM_CODE);

        assertEquals(PUPIL_EMAIL, response.email());
        assertEquals("detektiv1", response.username());
        assertEquals("Pupil Name", response.legalName());
        assertEquals(42L, response.avatarFileObjectId());
        assertEquals(CLASSROOM_CODE, response.classroomCode());
        assertTrue(response.membershipConfirmedByTeacher());
        assertEquals(1L, response.chapterProgressions().getFirst().chapterId());
        assertEquals(3, response.chapterProgressions().getFirst().successfulRuns());
    }

    @DisplayName("Throws not found when pupil classroom information is missing")
    @Test
    void getPupilInformation_whenMembershipDoesNotExist_throwsEntityNotFoundException() {
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pupilService.getPupilInformation(PUPIL_EMAIL, CLASSROOM_CODE));

        assertEquals("Pupil membership not found", exception.getMessage());
    }

    @DisplayName("Returns pupil chapter progression for existing classroom membership")
    @Test
    void getPupilProgressionPerChapter_whenMembershipExists_returnsProgressionResponse() {
        PupilClassroomMembership membership = membership(true, "detektiv1", null);
        membership.setChapterProgressions(List.of(
                progression(1L, 1, 3, 0),
                progression(2L, 0, 1, 2)));
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));

        PupilProgressionResponse response = pupilService.getPupilProgressionPerChapter(PUPIL_EMAIL, CLASSROOM_CODE);

        assertEquals(2, response.chapterProgressions().size());
        assertEquals(1L, response.chapterProgressions().get(0).chapterId());
        assertEquals(2L, response.chapterProgressions().get(1).chapterId());
        assertEquals(1, response.chapterProgressions().get(1).highscore());
    }

    @DisplayName("Returns true only for confirmed pupil classroom memberships")
    @Test
    void checkPupilMembershipForClass_whenMembershipExists_returnsConfirmationState() {
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership(true, "detektiv1", null)));

        boolean member = pupilService.checkPupilMembershipForClass(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE));

        assertTrue(member);
    }

    @DisplayName("Returns false for missing or unconfirmed pupil classroom memberships")
    @Test
    void checkPupilMembershipForClass_whenMembershipIsMissingOrUnconfirmed_returnsFalse() {
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership(false, "detektiv1", null)))
                .thenReturn(Optional.empty());

        boolean unconfirmedMember = pupilService.checkPupilMembershipForClass(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE));
        boolean missingMember = pupilService.checkPupilMembershipForClass(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE));

        assertFalse(unconfirmedMember);
        assertFalse(missingMember);
    }

    @DisplayName("Creates unconfirmed classroom join request for existing account and classroom")
    @Test
    void requestToJoinClassroom_whenAccountAndClassroomExist_savesUnconfirmedMembership() {
        Account account = account(PUPIL_EMAIL);
        Classroom classroom = classroom(CLASSROOM_CODE);
        when(accountRepository.findById(PUPIL_EMAIL)).thenReturn(Optional.of(account));
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.empty());
        when(pupilMembershipRepository.save(any(PupilClassroomMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PupilResponse response = pupilService.requestToJoinClassroom(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE));

        ArgumentCaptor<PupilClassroomMembership> membershipCaptor =
                ArgumentCaptor.forClass(PupilClassroomMembership.class);
        verify(pupilMembershipRepository).save(membershipCaptor.capture());
        PupilClassroomMembership saved = membershipCaptor.getValue();
        assertSame(account, saved.getAccount());
        assertSame(classroom, saved.getClassroom());
        assertFalse(saved.isMembershipConfirmedByTeacher());
        assertEquals(PUPIL_EMAIL, response.email());
        assertFalse(response.membershipConfirmedByTeacher());
    }

    @DisplayName("Returns existing join request without creating duplicate membership")
    @Test
    void requestToJoinClassroom_whenMembershipAlreadyExists_returnsExistingMembership() {
        PupilClassroomMembership membership = membership(false, null, null);
        when(accountRepository.findById(PUPIL_EMAIL)).thenReturn(Optional.of(account(PUPIL_EMAIL)));
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom(CLASSROOM_CODE)));
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));

        PupilResponse response = pupilService.requestToJoinClassroom(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE));

        assertEquals(PUPIL_EMAIL, response.email());
        assertFalse(response.membershipConfirmedByTeacher());
        verify(pupilMembershipRepository, never()).save(any(PupilClassroomMembership.class));
    }

    @DisplayName("Rejects classroom join request when account does not exist")
    @Test
    void requestToJoinClassroom_whenAccountDoesNotExist_throwsEntityNotFoundException() {
        when(accountRepository.findById(PUPIL_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pupilService.requestToJoinClassroom(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE)));

        assertEquals("Account not found", exception.getMessage());
        verify(classroomRepository, never()).findById(any());
        verify(pupilMembershipRepository, never()).save(any());
    }

    @DisplayName("Rejects classroom join request when classroom does not exist")
    @Test
    void requestToJoinClassroom_whenClassroomDoesNotExist_throwsEntityNotFoundException() {
        when(accountRepository.findById(PUPIL_EMAIL)).thenReturn(Optional.of(account(PUPIL_EMAIL)));
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pupilService.requestToJoinClassroom(new PupilRequest(PUPIL_EMAIL, CLASSROOM_CODE)));

        assertEquals("Classroom not found", exception.getMessage());
        verify(pupilMembershipRepository, never()).save(any());
    }

    @DisplayName("Updates pupil username and avatar for existing classroom membership")
    @Test
    void updateClassroomMembership_whenMembershipExists_savesTrimmedUsernameAndAvatarFileObject() {
        PupilClassroomMembership membership = membership(false, null, null);
        FileObject avatar = avatar(99L);
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(avatarService.getOrCreateAvatar("boy_hair1_costume2")).thenReturn(avatar);
        when(pupilMembershipRepository.save(membership)).thenReturn(membership);

        PupilClassroomMembershipResponse response = pupilService.updateClassroomMembership(
                PUPIL_EMAIL,
                CLASSROOM_CODE,
                "  detektiv1  ",
                "boy_hair1_costume2");

        assertEquals("detektiv1", membership.getUsername());
        assertSame(avatar, membership.getAvatarFileObject());
        assertEquals("detektiv1", response.username());
        assertEquals(99L, response.avatarFileObjectId());
        assertFalse(response.membershipConfirmedByTeacher());
    }

    @DisplayName("Rejects membership update with blank username before loading membership")
    @Test
    void updateClassroomMembership_whenUsernameIsBlank_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pupilService.updateClassroomMembership(PUPIL_EMAIL, CLASSROOM_CODE, " ", "boy_hair1_costume2"));

        assertEquals("Username must not be blank", exception.getMessage());
        verify(pupilMembershipRepository, never()).findByAccountEmailAndClassroomClassroomCode(any(), any());
    }

    @DisplayName("Throws not found when updating a missing pupil classroom membership")
    @Test
    void updateClassroomMembership_whenMembershipDoesNotExist_throwsEntityNotFoundException() {
        when(pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCode(PUPIL_EMAIL, CLASSROOM_CODE))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pupilService.updateClassroomMembership(PUPIL_EMAIL, CLASSROOM_CODE, "detektiv1", "boy_hair1_costume2"));

        assertEquals("Pupil membership not found", exception.getMessage());
        verify(avatarService, never()).getOrCreateAvatar(any());
    }

    private Account account(String email) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName("Pupil Name");
        account.setAccountRole(AccountRole.PUPIL);
        account.setPasswordData("encoded-password");
        return account;
    }

    private Classroom classroom(String classroomCode) {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(classroomCode);
        classroom.setClassroomName("8A");
        return classroom;
    }

    private PupilClassroomMembership membership(boolean confirmed, String username, FileObject avatar) {
        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(PUPIL_EMAIL);
        membership.setAccount(account(PUPIL_EMAIL));
        membership.setClassroom(classroom(CLASSROOM_CODE));
        membership.setUsername(username);
        membership.setMembershipConfirmedByTeacher(confirmed);
        membership.setAvatarFileObject(avatar);
        membership.setChapterProgressions(List.of());
        return membership;
    }

    private PupilChapterBridgeProgression progression(Long chapterId,
                                                      Integer successfulRuns,
                                                      Integer highscore,
                                                      Integer unsuccessfulStreak) {
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setChapterName("Chapter " + chapterId);
        chapter.setSuccessfulRunsNeededForCompletion(3);

        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, chapterId));
        progression.setChapter(chapter);
        progression.setSuccessfulRuns(successfulRuns);
        progression.setHighscore(highscore);
        progression.setUnsuccessfulStreak(unsuccessfulStreak);
        return progression;
    }

    private FileObject avatar(Long id) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey("avatar-images/boy_hair1_costume2.png");
        fileObject.setFileName("boy_hair1_costume2.png");
        return fileObject;
    }
}
