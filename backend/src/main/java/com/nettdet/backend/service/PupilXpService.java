package com.nettdet.backend.service;

import org.springframework.stereotype.Service;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;

@Service
public class PupilXpService {

    private static final int XP_PER_HIGHSCORE_POINT = 30;
    private static final int XP_PER_COMPLETED_CHAPTER = 30;

    public long calculateMembershipXp(PupilClassroomMembership membership) {
        long totalXp = 0L;

        for (PupilChapterBridgeProgression progression : membership.getChapterProgressions()) {
            int highscore = progression.getHighscore() == null ? 0 : progression.getHighscore();
            int successfulRuns = progression.getSuccessfulRuns() == null ? 0 : progression.getSuccessfulRuns();

            totalXp += (long) highscore * XP_PER_HIGHSCORE_POINT;
            if (successfulRuns >= 1) {
                totalXp += XP_PER_COMPLETED_CHAPTER;
            }
        }

        return totalXp;
    }
}
