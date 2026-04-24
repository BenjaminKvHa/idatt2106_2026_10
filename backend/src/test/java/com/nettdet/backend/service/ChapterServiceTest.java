package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterProgressionResponse;
import com.nettdet.backend.dto.ChapterResponse;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";
    private static final Long CHAPTER_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        chapterService = new ChapterService(
                taskRepository,
                membershipRepository,
                progressionRepository,
                chapterRepository,
                objectMapper,
                List.of());
    }

    @DisplayName("Returns all chapters in repository order as chapter responses")
    @Test
    void getAllChapters_whenChaptersExist_returnsChapterResponsesInRepositoryOrder() {
        when(chapterRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                chapter(1L, "Nyhetskvartalet"),
                chapter(2L, "Fotografen"),
                chapter(3L, "Datasenteret")));

        List<ChapterResponse> response = chapterService.getAllChapters();

        assertEquals(List.of(1L, 2L, 3L), response.stream()
                .map(ChapterResponse::id)
                .toList());
        assertEquals(List.of("Nyhetskvartalet", "Fotografen", "Datasenteret"), response.stream()
                .map(ChapterResponse::chapterName)
                .toList());
    }

    @DisplayName("Returns zeroed chapter progression when pupil has no progression row")
    @Test
    void getProgression_whenProgressionIsMissing_returnsZeroedProgression() {
        when(progressionRepository.findById(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID)))
                .thenReturn(Optional.empty());

        ChapterProgressionResponse response = chapterService.getProgression(PUPIL_EMAIL, CHAPTER_ID);

        assertEquals(0, response.currentRunAttempts());
        assertEquals(0, response.currentRunCorrect());
        assertEquals(0, response.successfulRuns());
        assertEquals(0, response.highscore());
        assertEquals(0, response.unsuccessfulStreak());
    }

    @DisplayName("Returns stored chapter progression counters when progression row exists")
    @Test
    void getProgression_whenProgressionExists_returnsStoredProgressionCounters() {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID));
        progression.setCurrentRunAttempts(2);
        progression.setCurrentRunCorrect(1);
        progression.setSuccessfulRuns(3);
        progression.setHighscore(4);
        progression.setUnsuccessfulStreak(5);
        when(progressionRepository.findById(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID)))
                .thenReturn(Optional.of(progression));

        ChapterProgressionResponse response = chapterService.getProgression(PUPIL_EMAIL, CHAPTER_ID);

        assertEquals(2, response.currentRunAttempts());
        assertEquals(1, response.currentRunCorrect());
        assertEquals(3, response.successfulRuns());
        assertEquals(4, response.highscore());
        assertEquals(5, response.unsuccessfulStreak());
    }

    private Chapter chapter(Long id, String name) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setChapterName(name);
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }
}
