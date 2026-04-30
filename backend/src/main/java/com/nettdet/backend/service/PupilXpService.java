package com.nettdet.backend.service;

import org.springframework.stereotype.Service;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;

/**
 * Pure-function service that derives a pupil's total XP from their chapter progressions.
 * <p>
 * The formula is intentionally simple and deterministic so the same membership always yields
 * the same XP value:
 * <ul>
 *     <li>{@value #XP_PER_HIGHSCORE_POINT} XP per highscore point on each chapter, plus</li>
 *     <li>a flat {@value #XP_PER_COMPLETED_CHAPTER} XP completion bonus for any chapter where
 *         the pupil has at least one successful run.</li>
 * </ul>
 * Null highscore / successful-runs counters are treated as zero so a freshly-seeded
 * progression row contributes nothing. This service does not persist or mutate state;
 * leaderboard and profile views call it on the membership graphs they have already loaded.
 */
@Service
public class PupilXpService {

    private static final int XP_PER_HIGHSCORE_POINT = 30;
    private static final int XP_PER_COMPLETED_CHAPTER = 30;

    /**
     * Computes the total XP for the given membership by summing each chapter progression's
     * highscore-derived XP plus a completion bonus for chapters with at least one successful run.
     *
     * @param membership the membership whose chapter progressions are aggregated
     * @return the total XP, never negative
     */
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
