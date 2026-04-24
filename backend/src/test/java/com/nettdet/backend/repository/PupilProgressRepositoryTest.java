package com.nettdet.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.support.PostgresDataJpaTest;

@PostgresDataJpaTest
class PupilProgressRepositoryTest {

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Finds all chapters ordered by ascending chapter id")
    @Test
    void findAllByOrderByIdAsc_whenChaptersExist_returnsAscendingChapterIds() {
        insertChapter(3L, "Datasenteret");
        insertChapter(1L, "Nyhetskvartalet");
        insertChapter(2L, "Fotografen");

        List<Chapter> chapters = chapterRepository.findAllByOrderByIdAsc();

        assertEquals(List.of(1L, 2L, 3L), chapters.stream().map(Chapter::getId).toList());
    }

    @DisplayName("Finds only the requested pupil progressions ordered by chapter id")
    @Test
    void findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc_whenMultiplePupilsExist_returnsOnlyRequestedPupilInChapterOrder() {
        insertProgressionData();
        insertProgression("pupil@example.com", 2L, 1, 0);
        insertProgression("other@example.com", 1L, 5, 2);
        insertProgression("pupil@example.com", 1L, 3, 1);

        List<PupilChapterBridgeProgression> progressions = progressionRepository
                .findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc("pupil@example.com");

        assertEquals(2, progressions.size());
        assertEquals(List.of(1L, 2L), progressions.stream()
                .map(progression -> progression.getId().getChapterId())
                .toList());
        assertTrue(progressions.stream()
                .allMatch(progression -> progression.getId().getEmail().equals("pupil@example.com")));
    }

    @DisplayName("Finds one progression row by pupil email and chapter id")
    @Test
    void findByIdEmailAndIdChapterId_whenProgressionExists_returnsMatchingProgression() {
        insertProgressionData();
        insertProgression("pupil@example.com", 1L, 3, 1);

        Optional<PupilChapterBridgeProgression> progression = progressionRepository
                .findByIdEmailAndIdChapterId("pupil@example.com", 1L);

        assertTrue(progression.isPresent());
        assertEquals(3, progression.orElseThrow().getHighscore());
        assertEquals(1, progression.orElseThrow().getSuccessfulRuns());
    }

    private void insertProgressionData() {
        insertClassroom("ABCABC");
        insertPupilAccount("pupil@example.com", "Pupil Name");
        insertPupilAccount("other@example.com", "Other Pupil");
        insertMembership("pupil@example.com", "ABCABC", "detektiv1");
        insertMembership("other@example.com", "ABCABC", "detektiv2");
        insertChapter(1L, "Nyhetskvartalet");
        insertChapter(2L, "Fotografen");
    }

    private void insertClassroom(String classroomCode) {
        jdbcTemplate.update(
                "INSERT INTO classroom (classroom_code, classroom_name, classroom_description) VALUES (?, ?, ?)",
                classroomCode,
                "Test classroom",
                "Repository test classroom");
    }

    private void insertPupilAccount(String email, String legalName) {
        jdbcTemplate.update(
                "INSERT INTO account (email, password_data, legal_name, account_role) VALUES (?, ?, ?, ?::account_role_enum)",
                email,
                "password",
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

    private void insertChapter(Long chapterId, String chapterName) {
        jdbcTemplate.update(
                "INSERT INTO chapter (id, chapter_name, successful_runs_needed_for_completion) VALUES (?, ?, ?)",
                chapterId,
                chapterName,
                3);
    }

    private void insertProgression(String email, Long chapterId, int highscore, int successfulRuns) {
        jdbcTemplate.update(
                "INSERT INTO pupil_chapter_bridge_progression (email, chapter_id, highscore, successful_runs) VALUES (?, ?, ?, ?)",
                email,
                chapterId,
                highscore,
                successfulRuns);
    }
}
