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

@Service
public class ProgressService {

    private final ChapterRepository chapterRepository;
    private final PupilChapterBridgeProgressionRepository progressionRepository;

    public ProgressService(ChapterRepository chapterRepository,
                           PupilChapterBridgeProgressionRepository progressionRepository) {
        this.chapterRepository = chapterRepository;
        this.progressionRepository = progressionRepository;
    }

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
