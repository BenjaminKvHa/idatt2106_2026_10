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

/**
 * Application service that hands out chapter tips to pupils and pins them to the pupil's
 * notebook.
 * <p>
 * Each chapter has a static catalogue of tips defined in {@link ChapterTipsCatalog}. The
 * dispensing rules implemented here are:
 * <ul>
 *     <li>If the chapter has no configured tips, the pupil receives a "no tip" response with
 *         the {@code allTipsReceived} flag set to true.</li>
 *     <li>If the pupil has already completed the chapter at least once
 *         ({@code successfulRuns >= 1}), they have unlocked the entire catalogue: every tip is
 *         force-merged into their notebook (idempotently) and a random tip from the catalogue
 *         is returned.</li>
 *     <li>Otherwise, the first catalogue tip not yet present in the notebook is added and
 *         returned. If the notebook already contains every catalogue tip, a random tip is
 *         returned without further mutation.</li>
 * </ul>
 * Progression rows and notebook JSON are lazily initialised on first access. The
 * {@code changed} flag in the response distinguishes "we just gave you a new tip" from
 * "you already had this one" so the UI can surface a notification only on novel additions.
 */
@Service
public class TipService {

    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterTipsCatalog tipsCatalog;
    private final NotebookJsonService notebookJsonService;

    /**
     * Constructs a {@code TipService} with the dependencies required to resolve the pupil's
     * progression, mutate their notebook, and read the configured tip catalogue.
     *
     * @param progressionRepository repository for the per-pupil per-chapter progression rows
     * @param membershipRepository  repository used to resolve the pupil's confirmed membership
     * @param chapterRepository     repository used to load the requested chapter
     * @param tipsCatalog           static catalogue of tips per chapter
     * @param notebookJsonService   marshalling layer for notebook JSON parsing/serialising
     */
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

    /**
     * Returns a tip for the authenticated pupil on the given chapter, applying the dispensing
     * rules described on the class-level javadoc.
     * <p>
     * Has the side effect of inserting a freshly delivered tip into the pupil's notebook (and,
     * for completed chapters, force-merging the entire catalogue).
     *
     * @param chapterId the chapter for which a tip is requested
     * @param principal the authenticated pupil; must hold the {@code PUPIL} role
     * @return the tip itself plus flags for {@code changed} (was this tip newly added?) and
     *         {@code allTipsReceived} (does the pupil now have every configured tip?)
     * @throws AccessDeniedException   if the principal is not a pupil with a confirmed membership
     * @throws EntityNotFoundException if the chapter does not exist
     */
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

    /**
     * Force-merges the entire configured tip catalogue for the progression's chapter into the
     * progression's notebook JSON, in memory.
     * <p>
     * Called when a pupil has just completed a chapter so the notebook reflects every tip even
     * if they were never explicitly requested. Existing tips are preserved (no duplicates), and
     * the progression entity is updated in place — the caller is responsible for persisting it.
     * Silently no-ops on null/empty progressions or chapters with no configured tips.
     *
     * @param progression the progression whose notebook should be filled in; may be {@code null}
     */
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
