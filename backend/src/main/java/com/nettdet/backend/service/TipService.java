package com.nettdet.backend.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterTipResponse;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TipService {

    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterTipsCatalog tipsCatalog;
    private final NotebookJsonService notebookJsonService;

    public TipService(PupilChapterBridgeProgressionRepository progressionRepository,
                      PupilClassroomMembershipRepository membershipRepository,
                      ChapterRepository chapterRepository,
                      ChapterTipsCatalog tipsCatalog,
                      NotebookJsonService notebookJsonService) {
        this.progressionRepository = progressionRepository;
        this.membershipRepository = membershipRepository;
        this.chapterRepository = chapterRepository;
        this.tipsCatalog = tipsCatalog;
        this.notebookJsonService = notebookJsonService;
    }

    @Transactional
    public ChapterTipResponse getPupilTip(Long chapterId, JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);
        PupilClassroomMembership membership = getConfirmedPupilMembership(principal.email());
        Chapter chapter = getRequiredChapter(chapterId);
        PupilChapterBridgeProgression progression = getOrCreateProgression(membership, chapter);
        List<String> configuredTips = tipsCatalog.tipsForChapter(chapter.getId());

        if (configuredTips.isEmpty()) {
            return new ChapterTipResponse(chapter.getId(), null, false, true);
        }

        if (progression.getSuccessfulRuns() != null && progression.getSuccessfulRuns() >= 1) {
            addAllConfiguredTipsToNotebook(progression);
            progressionRepository.save(progression);
            return new ChapterTipResponse(chapter.getId(), randomTip(configuredTips), false, true);
        }

        ChapterNotebookData notebookData = notebookJsonService.parse(progression.getNotebookJson());
        List<String> missingTips = configuredTips.stream()
                .filter(tip -> !notebookData.tips().contains(tip))
                .toList();

        if (missingTips.isEmpty()) {
            return new ChapterTipResponse(chapter.getId(), randomTip(configuredTips), false, true);
        }

        String tip = missingTips.getFirst();
        NotebookJsonService.TipAddResult result = notebookJsonService.addTip(progression.getNotebookJson(), tip);
        progression.setNotebookJson(result.notebookJson());
        progressionRepository.save(progression);

        boolean allTipsReceived = result.notebookData().tips().containsAll(configuredTips);
        return new ChapterTipResponse(chapter.getId(), tip, result.changed(), allTipsReceived);
    }

    public void addAllConfiguredTipsToNotebook(PupilChapterBridgeProgression progression) {
        if (progression == null || progression.getChapter() == null || progression.getChapter().getId() == null) {
            return;
        }

        List<String> configuredTips = tipsCatalog.tipsForChapter(progression.getChapter().getId());
        if (configuredTips.isEmpty()) {
            return;
        }

        NotebookJsonService.TipAddResult result =
                notebookJsonService.addTips(progression.getNotebookJson(), configuredTips);
        if (result.changed()) {
            progression.setNotebookJson(result.notebookJson());
        }
    }

    private PupilChapterBridgeProgression getOrCreateProgression(PupilClassroomMembership membership, Chapter chapter) {
        return progressionRepository.findByIdEmailAndIdChapterId(membership.getAccountEmail(), chapter.getId())
                .orElseGet(() -> createDefaultProgression(membership, chapter));
    }

    private PupilChapterBridgeProgression createDefaultProgression(PupilClassroomMembership membership, Chapter chapter) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(membership.getAccountEmail(), chapter.getId()));
        progression.setPupilClassroomMembership(membership);
        progression.setChapter(chapter);
        progression.setSuccessfulRuns(0);
        progression.setHighscore(0);
        progression.setUnsuccessfulStreak(0);
        progression.setCurrentRunAttempts(0);
        progression.setCurrentRunCorrect(0);
        progression.setNotebookJson(notebookJsonService.defaultNotebookJson());
        return progressionRepository.save(progression);
    }

    private void verifyPupilPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null
                || !AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }
    }

    private PupilClassroomMembership getConfirmedPupilMembership(String pupilEmail) {
        return membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(pupilEmail.trim())
                .orElseThrow(() -> new AccessDeniedException("Confirmed classroom membership is required"));
    }

    private Chapter getRequiredChapter(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
    }

    private String randomTip(List<String> tips) {
        return tips.get(ThreadLocalRandom.current().nextInt(tips.size()));
    }
}
