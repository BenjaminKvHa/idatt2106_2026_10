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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterTipResponse;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class TipServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";
    private static final String CLASSROOM_CODE = "ABCABC";
    private static final Long CHAPTER_ID = 1L;

    @Mock
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChapterTipsCatalog tipsCatalog;

    private ObjectMapper objectMapper;
    private NotebookJsonService notebookJsonService;
    private TipService tipService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notebookJsonService = new NotebookJsonService(objectMapper);
        tipService = new TipService(
                progressionRepository,
                membershipRepository,
                chapterRepository,
                tipsCatalog,
                notebookJsonService);
    }

    @DisplayName("Returns first missing tip and saves it to the notebook")
    @Test
    void getPupilTip_whenNotebookMissesTips_returnsFirstMissingTipAndSavesIt() throws Exception {
        PupilChapterBridgeProgression progression = progression(notebookJson(List.of("Tip 1"), "Own notes"), 0);
        givenConfirmedPupilWithProgression(progression);
        when(tipsCatalog.tipsForChapter(CHAPTER_ID)).thenReturn(List.of("Tip 1", "Tip 2", "Tip 3"));
        when(progressionRepository.save(progression)).thenReturn(progression);

        ChapterTipResponse response = tipService.getPupilTip(CHAPTER_ID, pupilPrincipal());

        ChapterNotebookData stored = objectMapper.readValue(progression.getNotebookJson(), ChapterNotebookData.class);
        assertEquals(CHAPTER_ID, response.chapterId());
        assertEquals("Tip 2", response.tip());
        assertTrue(response.newlyAdded());
        assertFalse(response.allTipsReceived());
        assertEquals(List.of("Tip 1", "Tip 2"), stored.tips());
        assertEquals("Own notes", stored.ownNotes());
        verify(progressionRepository).save(progression);
    }

    @DisplayName("Does not duplicate tips when all tips are already in the notebook")
    @Test
    void getPupilTip_whenAllTipsAlreadyReceived_returnsExistingTipWithoutSavingDuplicate() throws Exception {
        PupilChapterBridgeProgression progression = progression(notebookJson(List.of("Tip 1", "Tip 2"), ""), 0);
        givenConfirmedPupilWithProgression(progression);
        when(tipsCatalog.tipsForChapter(CHAPTER_ID)).thenReturn(List.of("Tip 1", "Tip 2"));

        ChapterTipResponse response = tipService.getPupilTip(CHAPTER_ID, pupilPrincipal());

        ChapterNotebookData stored = objectMapper.readValue(progression.getNotebookJson(), ChapterNotebookData.class);
        assertTrue(List.of("Tip 1", "Tip 2").contains(response.tip()));
        assertFalse(response.newlyAdded());
        assertTrue(response.allTipsReceived());
        assertEquals(List.of("Tip 1", "Tip 2"), stored.tips());
        verify(progressionRepository, never()).save(progression);
    }

    @DisplayName("Adds all chapter tips before returning a tip when chapter is already completed")
    @Test
    void getPupilTip_whenChapterIsCompleted_addsAllConfiguredTipsToNotebook() throws Exception {
        PupilChapterBridgeProgression progression = progression(notebookJson(List.of("Tip 1"), "Own notes"), 1);
        givenConfirmedPupilWithProgression(progression);
        when(tipsCatalog.tipsForChapter(CHAPTER_ID)).thenReturn(List.of("Tip 1", "Tip 2", "Tip 3"));
        when(progressionRepository.save(progression)).thenReturn(progression);

        ChapterTipResponse response = tipService.getPupilTip(CHAPTER_ID, pupilPrincipal());

        ChapterNotebookData stored = objectMapper.readValue(progression.getNotebookJson(), ChapterNotebookData.class);
        assertTrue(List.of("Tip 1", "Tip 2", "Tip 3").contains(response.tip()));
        assertFalse(response.newlyAdded());
        assertTrue(response.allTipsReceived());
        assertEquals(List.of("Tip 1", "Tip 2", "Tip 3"), stored.tips());
    }

    @DisplayName("Auto-creates progression for confirmed pupil when progression is missing")
    @Test
    void getPupilTip_whenProgressionIsMissing_createsProgressionAndSavesTip() throws Exception {
        PupilClassroomMembership membership = membership();
        Chapter chapter = chapter();
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.empty());
        when(progressionRepository.save(any(PupilChapterBridgeProgression.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tipsCatalog.tipsForChapter(CHAPTER_ID)).thenReturn(List.of("Tip 1", "Tip 2"));

        ChapterTipResponse response = tipService.getPupilTip(CHAPTER_ID, pupilPrincipal());

        ArgumentCaptor<PupilChapterBridgeProgression> progressionCaptor =
                ArgumentCaptor.forClass(PupilChapterBridgeProgression.class);
        verify(progressionRepository, org.mockito.Mockito.times(2)).save(progressionCaptor.capture());
        PupilChapterBridgeProgression saved = progressionCaptor.getAllValues().getLast();
        ChapterNotebookData stored = objectMapper.readValue(saved.getNotebookJson(), ChapterNotebookData.class);
        assertEquals("Tip 1", response.tip());
        assertEquals(PUPIL_EMAIL, saved.getId().getEmail());
        assertEquals(CHAPTER_ID, saved.getId().getChapterId());
        assertEquals(List.of("Tip 1"), stored.tips());
    }

    @DisplayName("Returns null tip when no tips are configured for the chapter")
    @Test
    void getPupilTip_whenNoTipsAreConfigured_returnsEmptyTipResponse() {
        PupilChapterBridgeProgression progression = progression(notebookJson(List.of(), ""), 0);
        givenConfirmedPupilWithProgression(progression);
        when(tipsCatalog.tipsForChapter(CHAPTER_ID)).thenReturn(List.of());

        ChapterTipResponse response = tipService.getPupilTip(CHAPTER_ID, pupilPrincipal());

        assertNull(response.tip());
        assertFalse(response.newlyAdded());
        assertTrue(response.allTipsReceived());
    }

    @DisplayName("Rejects tip access from unconfirmed pupils")
    @Test
    void getPupilTip_whenPupilIsNotConfirmed_throwsAccessDenied() {
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> tipService.getPupilTip(CHAPTER_ID, pupilPrincipal()));

        verifyNoInteractions(chapterRepository, progressionRepository, tipsCatalog);
    }

    @DisplayName("Rejects tip access from teacher principals")
    @Test
    void getPupilTip_whenPrincipalIsTeacher_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        assertThrows(AccessDeniedException.class, () -> tipService.getPupilTip(CHAPTER_ID, principal));

        verifyNoInteractions(membershipRepository, chapterRepository, progressionRepository, tipsCatalog);
    }

    @DisplayName("Adds all configured tips to a progression without duplicates")
    @Test
    void addAllConfiguredTipsToNotebook_whenSomeTipsExist_addsOnlyMissingTips() throws Exception {
        PupilChapterBridgeProgression progression = progression(notebookJson(List.of("Tip 1"), "Own notes"), 0);
        when(tipsCatalog.tipsForChapter(CHAPTER_ID)).thenReturn(List.of("Tip 1", "Tip 2"));

        tipService.addAllConfiguredTipsToNotebook(progression);

        ChapterNotebookData stored = objectMapper.readValue(progression.getNotebookJson(), ChapterNotebookData.class);
        assertEquals(List.of("Tip 1", "Tip 2"), stored.tips());
        assertEquals("Own notes", stored.ownNotes());
    }

    private void givenConfirmedPupilWithProgression(PupilChapterBridgeProgression progression) {
        when(membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(PUPIL_EMAIL))
                .thenReturn(Optional.of(membership()));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter()));
        when(progressionRepository.findByIdEmailAndIdChapterId(PUPIL_EMAIL, CHAPTER_ID))
                .thenReturn(Optional.of(progression));
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, AccountRole.PUPIL.name());
    }

    private PupilChapterBridgeProgression progression(String notebookJson, int successfulRuns) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID));
        progression.setPupilClassroomMembership(membership());
        progression.setChapter(chapter());
        progression.setSuccessfulRuns(successfulRuns);
        progression.setHighscore(0);
        progression.setUnsuccessfulStreak(0);
        progression.setCurrentRunAttempts(0);
        progression.setCurrentRunCorrect(0);
        progression.setNotebookJson(notebookJson);
        return progression;
    }

    private PupilClassroomMembership membership() {
        Account account = new Account();
        account.setEmail(PUPIL_EMAIL);
        account.setLegalName("Pupil Name");
        account.setAccountRole(AccountRole.PUPIL);
        account.setPasswordData("encoded-password");

        Classroom classroom = new Classroom();
        classroom.setClassroomCode(CLASSROOM_CODE);
        classroom.setClassroomName("Classroom");

        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(PUPIL_EMAIL);
        membership.setAccount(account);
        membership.setClassroom(classroom);
        membership.setUsername("detektiv");
        membership.setMembershipConfirmedByTeacher(true);
        return membership;
    }

    private Chapter chapter() {
        Chapter chapter = new Chapter();
        chapter.setId(CHAPTER_ID);
        chapter.setChapterName("Nyhetskvartalet");
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }

    private String notebookJson(List<String> tips, String ownNotes) {
        try {
            return objectMapper.writeValueAsString(new ChapterNotebookData(tips, ownNotes));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create notebook test JSON", exception);
        }
    }
}
