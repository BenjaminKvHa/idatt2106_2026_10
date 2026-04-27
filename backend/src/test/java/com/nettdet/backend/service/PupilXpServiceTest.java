package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;

class PupilXpServiceTest {

    private final PupilXpService pupilXpService = new PupilXpService();

    @DisplayName("Calculates XP from highscore points and completed chapters")
    @Test
    void calculateMembershipXp_whenProgressionsHaveScoresAndCompletions_returnsLeaderboardXp() {
        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setChapterProgressions(List.of(
                progression(3, 1),
                progression(2, 0),
                progression(null, null)));

        long totalXp = pupilXpService.calculateMembershipXp(membership);

        assertEquals(180L, totalXp);
    }

    private PupilChapterBridgeProgression progression(Integer highscore, Integer successfulRuns) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setHighscore(highscore);
        progression.setSuccessfulRuns(successfulRuns);
        return progression;
    }
}
