package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterNotebookResponse;
import com.nettdet.backend.dto.UpdateChapterNotebookRequest;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class NotebookServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";
    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final String CLASSROOM_CODE = "ABCABC";
    private static final Long CHAPTER_ID = 2L;

    @Mock
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    private ObjectMapper objectMapper;
    private NotebookService notebookService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notebookService = new NotebookService(
                progressionRepository,
                membershipRepository,
                chapterRepository,
                teacherBridgeRepository,
                new NotebookJsonService(objectMapper));
    }

    @DisplayName("Returns existing pupil notebook for confirmed classroom membership")
    @Test
    void getPupilNotebook_whenProgressionExists_returnsNotebookData() {
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        PupilChapterBridgeProgression progression = progression(membership, chapter(CHAPTER_ID), notebookJson(
                List.of("Check source", "Ask teacher"),
                "My notes"));
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter(CHAPTER_ID)));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));

        ChapterNotebookResponse response = notebookService.getPupilNotebook(CHAPTER_ID, pupilPrincipal());

        assertEquals(CHAPTER_ID, response.chapterId());
        assertEquals("Chapter 2", response.chapterName());
        assertEquals(List.of("Check source", "Ask teacher"), response.tips());
        assertEquals("My notes", response.ownNotes());
    }

    @DisplayName("Auto-creates default progression when confirmed pupil has no chapter progression")
    @Test
    void getPupilNotebook_whenProgressionIsMissing_createsDefaultProgression() {
        Chapter chapter = chapter(CHAPTER_ID);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.empty());
        when(progressionRepository.save(any(PupilChapterBridgeProgression.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChapterNotebookResponse response = notebookService.getPupilNotebook(CHAPTER_ID, pupilPrincipal());

        ArgumentCaptor<PupilChapterBridgeProgression> progressionCaptor =
                ArgumentCaptor.forClass(PupilChapterBridgeProgression.class);
        verify(progressionRepository).save(progressionCaptor.capture());
        PupilChapterBridgeProgression saved = progressionCaptor.getValue();
        assertEquals(PUPIL_EMAIL, saved.getId().getEmail());
        assertEquals(CHAPTER_ID, saved.getId().getChapterId());
        assertEquals(0, saved.getSuccessfulRuns());
        assertEquals(0, saved.getHighscore());
        assertEquals(0, saved.getUnsuccessfulStreak());
        assertEquals(0, saved.getCurrentRunAttempts());
        assertEquals(0, saved.getCurrentRunCorrect());
        assertEquals("Chapter 2", response.chapterName());
        assertEquals(List.of(), response.tips());
        assertEquals("", response.ownNotes());
    }

    @DisplayName("Auto-creates default progression before updating notes when pupil progression is missing")
    @Test
    void updatePupilNotebook_whenProgressionIsMissing_createsDefaultProgressionAndSavesNotes() throws Exception {
        Chapter chapter = chapter(CHAPTER_ID);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.empty());
        when(progressionRepository.save(any(PupilChapterBridgeProgression.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChapterNotebookResponse response = notebookService.updatePupilNotebook(
                CHAPTER_ID,
                new UpdateChapterNotebookRequest("New notes"),
                pupilPrincipal());

        ArgumentCaptor<PupilChapterBridgeProgression> progressionCaptor =
                ArgumentCaptor.forClass(PupilChapterBridgeProgression.class);
        verify(progressionRepository, org.mockito.Mockito.times(2)).save(progressionCaptor.capture());
        PupilChapterBridgeProgression saved = progressionCaptor.getAllValues().getLast();
        ChapterNotebookData stored = objectMapper.readValue(saved.getNotebookJson(), ChapterNotebookData.class);
        assertEquals(0, saved.getCurrentRunAttempts());
        assertEquals(0, saved.getCurrentRunCorrect());
        assertEquals("New notes", response.ownNotes());
        assertEquals("New notes", stored.ownNotes());
    }

    @DisplayName("Initializes blank notebook JSON on existing progression before returning notebook")
    @Test
    void getPupilNotebook_whenExistingProgressionHasBlankNotebook_initializesNotebookJson() {
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        PupilChapterBridgeProgression progression = progression(membership, chapter(CHAPTER_ID), " ");
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter(CHAPTER_ID)));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));
        when(progressionRepository.save(progression)).thenReturn(progression);

        ChapterNotebookResponse response = notebookService.getPupilNotebook(CHAPTER_ID, pupilPrincipal());

        assertEquals("Chapter 2", response.chapterName());
        assertEquals(List.of(), response.tips());
        assertEquals("", response.ownNotes());
        assertTrue(progression.getNotebookJson().contains("tips"));
        verify(progressionRepository).save(progression);
    }

    @DisplayName("Updates only pupil own notes and preserves read-only tips")
    @Test
    void updatePupilNotebook_whenProgressionExists_updatesOwnNotesAndPreservesTips() throws Exception {
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        PupilChapterBridgeProgression progression = progression(membership, chapter(CHAPTER_ID), notebookJson(
                List.of("Read source carefully"),
                "Old notes"));
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter(CHAPTER_ID)));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));
        when(progressionRepository.save(progression)).thenReturn(progression);

        ChapterNotebookResponse response = notebookService.updatePupilNotebook(
                CHAPTER_ID,
                new UpdateChapterNotebookRequest("New own notes"),
                pupilPrincipal());

        ChapterNotebookData stored = objectMapper.readValue(progression.getNotebookJson(), ChapterNotebookData.class);
        assertEquals("Chapter 2", response.chapterName());
        assertEquals(List.of("Read source carefully"), response.tips());
        assertEquals("New own notes", response.ownNotes());
        assertEquals(List.of("Read source carefully"), stored.tips());
        assertEquals("New own notes", stored.ownNotes());
    }

    @DisplayName("Normalizes null pupil own notes to an empty string")
    @Test
    void updatePupilNotebook_whenOwnNotesIsNull_storesEmptyOwnNotes() {
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        PupilChapterBridgeProgression progression = progression(membership, chapter(CHAPTER_ID), notebookJson(
                List.of("Tip"),
                "Old notes"));
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter(CHAPTER_ID)));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));
        when(progressionRepository.save(progression)).thenReturn(progression);

        ChapterNotebookResponse response = notebookService.updatePupilNotebook(
                CHAPTER_ID,
                new UpdateChapterNotebookRequest(null),
                pupilPrincipal());

        assertEquals("", response.ownNotes());
    }

    @DisplayName("Rejects pupil notebook access without confirmed classroom membership")
    @Test
    void getPupilNotebook_whenMembershipIsNotConfirmed_throwsAccessDenied() {
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> notebookService.getPupilNotebook(CHAPTER_ID, pupilPrincipal()));

        verifyNoInteractions(chapterRepository, progressionRepository);
    }

    @DisplayName("Rejects pupil notebook access for teacher principals")
    @Test
    void getPupilNotebook_whenPrincipalIsTeacher_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", TEACHER_EMAIL, AccountRole.TEACHER.name());

        assertThrows(AccessDeniedException.class, () -> notebookService.getPupilNotebook(CHAPTER_ID, principal));

        verifyNoInteractions(membershipRepository, chapterRepository, progressionRepository);
    }

    @DisplayName("Returns not found when notebook chapter does not exist")
    @Test
    void getPupilNotebook_whenChapterDoesNotExist_throwsEntityNotFoundException() {
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE))));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> notebookService.getPupilNotebook(CHAPTER_ID, pupilPrincipal()));

        assertEquals("Chapter not found", exception.getMessage());
        verify(progressionRepository, never()).findByIdEmailAndIdChapterId(any(), any());
    }

    @DisplayName("Allows teachers to read notebooks for confirmed pupils in classrooms they teach")
    @Test
    void getTeacherNotebook_whenTeacherOwnsClassroomAndPupilIsConfirmed_returnsNotebookData() {
        Classroom classroom = classroom(CLASSROOM_CODE);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom);
        PupilChapterBridgeProgression progression = progression(membership, chapter(CHAPTER_ID), notebookJson(
                List.of("Teacher-visible tip"),
                "Pupil note"));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                PUPIL_EMAIL,
                CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter(CHAPTER_ID)));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));

        ChapterNotebookResponse response = notebookService.getTeacherNotebook(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                CHAPTER_ID,
                teacherPrincipal());

        assertEquals("Chapter 2", response.chapterName());
        assertEquals(List.of("Teacher-visible tip"), response.tips());
        assertEquals("Pupil note", response.ownNotes());
    }

    @DisplayName("Auto-creates default progression when teacher opens notebook for confirmed pupil without progression")
    @Test
    void getTeacherNotebook_whenProgressionIsMissing_createsDefaultProgression() {
        Classroom classroom = classroom(CLASSROOM_CODE);
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom);
        Chapter chapter = chapter(CHAPTER_ID);
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(true);
        when(membershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                PUPIL_EMAIL,
                CLASSROOM_CODE))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.empty());
        when(progressionRepository.save(any(PupilChapterBridgeProgression.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChapterNotebookResponse response = notebookService.getTeacherNotebook(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                CHAPTER_ID,
                teacherPrincipal());

        ArgumentCaptor<PupilChapterBridgeProgression> progressionCaptor =
                ArgumentCaptor.forClass(PupilChapterBridgeProgression.class);
        verify(progressionRepository).save(progressionCaptor.capture());
        PupilChapterBridgeProgression saved = progressionCaptor.getValue();
        assertEquals(0, saved.getCurrentRunAttempts());
        assertEquals(0, saved.getCurrentRunCorrect());
        assertEquals("Chapter 2", response.chapterName());
        assertEquals(List.of(), response.tips());
        assertEquals("", response.ownNotes());
    }

    @DisplayName("Rejects teacher notebook access when teacher does not belong to classroom")
    @Test
    void getTeacherNotebook_whenTeacherDoesNotOwnClassroom_throwsAccessDenied() {
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> notebookService.getTeacherNotebook(CLASSROOM_CODE, PUPIL_EMAIL, CHAPTER_ID, teacherPrincipal()));

        verifyNoInteractions(membershipRepository, chapterRepository, progressionRepository);
    }

    @DisplayName("Returns empty notebook data for malformed notebook JSON")
    @Test
    void getPupilNotebook_whenNotebookJsonIsMalformed_returnsEmptyNotebookData() {
        PupilClassroomMembership membership = membership(PUPIL_EMAIL, classroom(CLASSROOM_CODE));
        PupilChapterBridgeProgression progression = progression(membership, chapter(CHAPTER_ID), "{not valid json");
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter(CHAPTER_ID)));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));

        ChapterNotebookResponse response = notebookService.getPupilNotebook(CHAPTER_ID, pupilPrincipal());

        assertEquals(List.of(), response.tips());
        assertEquals("", response.ownNotes());
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, AccountRole.PUPIL.name());
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", TEACHER_EMAIL, AccountRole.TEACHER.name());
    }

    private Classroom classroom(String classroomCode) {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(classroomCode);
        classroom.setClassroomName("8A");
        return classroom;
    }

    private Chapter chapter(Long chapterId) {
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setChapterName("Chapter " + chapterId);
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }

    private PupilClassroomMembership membership(String email, Classroom classroom) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName("Pupil Name");
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

    private PupilChapterBridgeProgression progression(PupilClassroomMembership membership,
                                                      Chapter chapter,
                                                      String notebookJson) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(membership.getAccountEmail(), chapter.getId()));
        progression.setPupilClassroomMembership(membership);
        progression.setChapter(chapter);
        progression.setSuccessfulRuns(0);
        progression.setHighscore(0);
        progression.setUnsuccessfulStreak(0);
        progression.setNotebookJson(notebookJson);
        return progression;
    }

    private String notebookJson(List<String> tips, String ownNotes) {
        try {
            return objectMapper.writeValueAsString(new ChapterNotebookData(tips, ownNotes));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to create notebook test JSON", exception);
        }
    }
}

