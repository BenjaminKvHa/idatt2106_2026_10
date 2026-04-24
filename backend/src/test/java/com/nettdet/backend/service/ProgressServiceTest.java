package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.PupilChapterProgressResponse;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @InjectMocks
    private ProgressService progressService;

    @DisplayName("Rejects null pupil progress principal")
    @Test
    void getPupilProgress_whenPrincipalIsNull_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> progressService.getPupilProgress(null));

        verifyNoInteractions(chapterRepository, progressionRepository);
    }

    @DisplayName("Rejects pupil progress principal without email")
    @Test
    void getPupilProgress_whenPrincipalHasMissingEmail_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", null, "PUPIL");

        assertThrows(AccessDeniedException.class, () -> progressService.getPupilProgress(principal));

        verifyNoInteractions(chapterRepository, progressionRepository);
    }

    @DisplayName("Rejects non-pupil principal for pupil progress")
    @Test
    void getPupilProgress_whenPrincipalIsNotPupil_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        assertThrows(AccessDeniedException.class, () -> progressService.getPupilProgress(principal));

        verifyNoInteractions(chapterRepository, progressionRepository);
    }

    @DisplayName("Returns all chapters with highscores and unlocks based on previous successful runs")
    @Test
    void getPupilProgress_whenChaptersHaveMixedProgress_returnsUnlocksAndHighscoresByChapterOrder() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(chapterRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(chapter(1L), chapter(2L), chapter(3L), chapter(4L)));
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of(
                        progression(1L, 3, 1),
                        progression(2L, 1, 0),
                        progression(4L, 5, 4)));

        List<PupilChapterProgressResponse> response = progressService.getPupilProgress(principal);

        assertEquals(4, response.size());
        assertProgress(response.get(0), 1L, 3, true);
        assertProgress(response.get(1), 2L, 1, true);
        assertProgress(response.get(2), 3L, 0, false);
        assertProgress(response.get(3), 4L, 5, false);

        verify(progressionRepository).findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL);
    }

    @DisplayName("Returns all chapters with only the first unlocked when no progression rows exist")
    @Test
    void getPupilProgress_whenPupilHasNoProgressionRows_returnsAllChaptersWithOnlyFirstUnlocked() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(chapterRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(chapter(1L), chapter(2L), chapter(3L)));
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of());

        List<PupilChapterProgressResponse> response = progressService.getPupilProgress(principal);

        assertEquals(3, response.size());
        assertProgress(response.get(0), 1L, 0, true);
        assertProgress(response.get(1), 2L, 0, false);
        assertProgress(response.get(2), 3L, 0, false);
    }

    @DisplayName("Unlocks the next chapter when previous chapter has one successful run")
    @Test
    void getPupilProgress_whenPreviousSuccessfulRunsIsOne_unlocksNextChapter() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(chapterRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(chapter(1L), chapter(2L)));
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of(progression(1L, 0, 1)));

        List<PupilChapterProgressResponse> response = progressService.getPupilProgress(principal);

        assertTrue(response.get(1).unlocked());
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, "PUPIL");
    }

    private Chapter chapter(Long id) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setChapterName("Chapter " + id);
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }

    private PupilChapterBridgeProgression progression(Long chapterId, Integer highscore, Integer successfulRuns) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, chapterId));
        progression.setHighscore(highscore);
        progression.setSuccessfulRuns(successfulRuns);
        return progression;
    }

    private void assertProgress(PupilChapterProgressResponse response,
                                Long chapterId,
                                int highscore,
                                boolean unlocked) {
        assertEquals(chapterId, response.chapterId());
        assertEquals(highscore, response.highscore());
        assertEquals(unlocked, response.unlocked());
    }
}
