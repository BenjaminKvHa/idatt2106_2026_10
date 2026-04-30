package com.nettdet.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.PupilChapterProgressResponse;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

/**
 * Read model for the authenticated pupil's chapter map.
 * <p>
 * Combines the global chapter catalogue (ordered by id) with the pupil's progression rows to
 * produce one entry per chapter, each carrying the pupil's current highscore and an
 * {@code unlocked} flag. The unlock rule is intentionally simple: the first chapter is always
 * unlocked, and every subsequent chapter is unlocked iff the previous chapter has at least one
 * successful run on the pupil's progression row. Chapters the pupil has never started count as
 * zero successful runs and zero highscore.
 * <p>
 * This service is read-only and never creates progression rows; rows are materialised by
 * gameplay services such as {@link TipService} and {@link NotebookService}.
 */
@Service
public class ProgressService {

    private final ChapterRepository chapterRepository;
    private final PupilChapterBridgeProgressionRepository progressionRepository;

    /**
     * Constructs a {@code ProgressService} with the repositories needed to assemble the
     * per-chapter progression view.
     *
     * @param chapterRepository      repository for the global chapter catalogue
     * @param progressionRepository  repository for the pupil's per-chapter progression rows
     */
    public ProgressService(ChapterRepository chapterRepository,
                           PupilChapterBridgeProgressionRepository progressionRepository) {
        this.chapterRepository = chapterRepository;
        this.progressionRepository = progressionRepository;
    }

    /**
     * Returns the authenticated pupil's progression for every chapter, in chapter-id order.
     * <p>
     * Each entry carries the pupil's highscore (0 if no progression row exists) and an
     * {@code unlocked} flag derived sequentially: chapter <em>n</em> is unlocked iff
     * <em>n</em> is the first chapter or the pupil has at least one successful run on
     * chapter <em>n-1</em>.
     *
     * @param principal the authenticated pupil; must hold the {@code PUPIL} role
     * @return one progression entry per chapter, ordered by chapter id ascending
     * @throws AccessDeniedException if the principal is not a pupil
     */
    @Transactional(readOnly = true)
    public List<PupilChapterProgressResponse> getPupilProgress(JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);

        List<Chapter> chapters = chapterRepository.findAllByOrderByIdAsc();
        Map<Long, PupilChapterBridgeProgression> progressionsByChapterId = new HashMap<>();

        for (PupilChapterBridgeProgression progression :
                progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(principal.email())) {
            progressionsByChapterId.put(progression.getId().getChapterId(), progression);
        }

        List<PupilChapterProgressResponse> response = new java.util.ArrayList<>();
        int previousSuccessfulRuns = 0;

        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            PupilChapterBridgeProgression progression = progressionsByChapterId.get(chapter.getId());
            int highscore = progression != null && progression.getHighscore() != null
                    ? progression.getHighscore()
                    : 0;
            boolean unlocked = i == 0 || previousSuccessfulRuns >= 1;

            response.add(new PupilChapterProgressResponse(chapter.getId(), highscore, unlocked));

            previousSuccessfulRuns = progression != null && progression.getSuccessfulRuns() != null
                    ? progression.getSuccessfulRuns()
                    : 0;
        }

        return response;
    }

    private void verifyPupilPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Pupil access is required");
        }

        if (!AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }
    }
}
