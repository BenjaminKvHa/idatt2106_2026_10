package com.nettdet.backend.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterNotebookResponse;
import com.nettdet.backend.dto.UpdateChapterNotebookRequest;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

/**
 * Application service for the per-pupil, per-chapter notebook.
 * <p>
 * Each pupil has one notebook per chapter, stored as a JSON document on the
 * {@code pupil_chapter_bridge_progression} row. The notebook holds two things: the tips the
 * pupil has unlocked while playing the chapter, and the pupil's own free-form notes.
 * <p>
 * This service exposes three operations:
 * <ul>
 *     <li><b>Pupil read</b> – the owning pupil retrieves their notebook for a chapter.</li>
 *     <li><b>Pupil update</b> – the owning pupil overwrites their {@code ownNotes} (tips are
 *         not editable from this endpoint; they are added by {@link TipService}).</li>
 *     <li><b>Teacher read</b> – a teacher inspects the notebook of one of their confirmed
 *         pupils, scoped to a classroom they teach.</li>
 * </ul>
 * Progression rows and notebook JSON are lazily initialised on first access, so reading a
 * chapter the pupil has never played still returns a well-formed empty notebook.
 * <p>
 * Authorisation is enforced in this layer, not just by Spring Security: pupils must hold the
 * {@code PUPIL} role and a teacher-confirmed classroom membership; teachers must hold the
 * {@code TEACHER} role and be linked to the requested classroom.
 */
@Service
public class NotebookService {

    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final ChapterRepository chapterRepository;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    private final NotebookJsonService notebookJsonService;

    /**
     * Constructs a {@code NotebookService} with all collaborators required to resolve, persist
     * and authorise notebook access.
     *
     * @param progressionRepository repository for the per-pupil per-chapter progression rows
     *                              that hold the notebook JSON column
     * @param membershipRepository  repository used to resolve the requesting pupil's confirmed
     *                              classroom membership
     * @param chapterRepository     repository used to load the requested chapter
     * @param teacherBridgeRepository repository used to verify teacher↔classroom ownership
     * @param notebookJsonService   marshalling layer for notebook JSON parsing/serialising
     */
    public NotebookService(PupilChapterBridgeProgressionRepository progressionRepository,
                           PupilClassroomMembershipRepository membershipRepository,
                           ChapterRepository chapterRepository,
                           AccountClassroomBridgeTeacherRepository teacherBridgeRepository,
                           NotebookJsonService notebookJsonService) {
        this.progressionRepository = progressionRepository;
        this.membershipRepository = membershipRepository;
        this.chapterRepository = chapterRepository;
        this.teacherBridgeRepository = teacherBridgeRepository;
        this.notebookJsonService = notebookJsonService;
    }

    /**
     * Returns the authenticated pupil's notebook for the given chapter.
     * <p>
     * If the pupil has no progression row for this chapter yet, one is created on the fly
     * with default values and an empty notebook so the response is always well-formed.
     *
     * @param chapterId the chapter whose notebook is being read
     * @param principal the authenticated pupil; must hold the {@code PUPIL} role
     * @return the chapter's tips and own-notes for this pupil
     * @throws AccessDeniedException if the principal is not a pupil with a confirmed membership
     * @throws EntityNotFoundException if the chapter does not exist
     */
    @Transactional
    public ChapterNotebookResponse getPupilNotebook(Long chapterId, JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);
        PupilClassroomMembership membership = getConfirmedPupilMembership(principal.email());
        Chapter chapter = getRequiredChapter(chapterId);
        PupilChapterBridgeProgression progression = getOrCreateProgression(membership, chapter);
        return toResponse(chapter, progression);
    }

    /**
     * Overwrites the {@code ownNotes} portion of the authenticated pupil's notebook for the
     * given chapter. The unlocked tip list is preserved verbatim.
     * <p>
     * A {@code null} {@code ownNotes} field on the request is normalised to an empty string,
     * so this endpoint cannot be used to "clear" tips — only to edit free-form notes.
     *
     * @param chapterId the chapter whose notebook is being mutated
     * @param request   carries the new {@code ownNotes} value
     * @param principal the authenticated pupil; must hold the {@code PUPIL} role
     * @return the persisted notebook after the update
     * @throws AccessDeniedException if the principal is not a pupil with a confirmed membership
     * @throws EntityNotFoundException if the chapter does not exist
     */
    @Transactional
    public ChapterNotebookResponse updatePupilNotebook(Long chapterId,
                                                       UpdateChapterNotebookRequest request,
                                                       JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);
        PupilClassroomMembership membership = getConfirmedPupilMembership(principal.email());
        Chapter chapter = getRequiredChapter(chapterId);
        PupilChapterBridgeProgression progression = getOrCreateProgression(membership, chapter);

        ChapterNotebookData existing = notebookJsonService.parse(progression.getNotebookJson());
        ChapterNotebookData updated = new ChapterNotebookData(existing.tips(), normalizeOwnNotes(request.ownNotes()));
        progression.setNotebookJson(notebookJsonService.serialize(updated));

        return toResponse(chapter, progressionRepository.save(progression));
    }

    /**
     * Returns the notebook of a specific pupil in a classroom, viewed by an authenticated teacher.
     * <p>
     * The teacher must be linked to {@code classroomCode}, and the pupil's membership in that
     * classroom must be teacher-confirmed. If no progression row exists yet for that pupil and
     * chapter, one is created on the fly with default values.
     *
     * @param classroomCode the classroom code that scopes the lookup
     * @param pupilEmail    the email of the pupil whose notebook is requested
     * @param chapterId     the chapter whose notebook is being read
     * @param principal     the authenticated teacher; must hold the {@code TEACHER} role
     * @return the pupil's notebook for the chapter
     * @throws AccessDeniedException if the principal is not a teacher of the classroom
     * @throws EntityNotFoundException if the chapter or the confirmed pupil membership is missing
     */
    @Transactional
    public ChapterNotebookResponse getTeacherNotebook(String classroomCode,
                                                      String pupilEmail,
                                                      Long chapterId,
                                                      JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        verifyTeacherOwnership(principal.email(), classroomCode);

        PupilClassroomMembership membership = membershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                        normalizeEmail(pupilEmail),
                        classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Confirmed pupil membership not found"));

        Chapter chapter = getRequiredChapter(chapterId);
        PupilChapterBridgeProgression progression = getOrCreateProgression(membership, chapter);
        return toResponse(chapter, progression);
    }

    private void verifyPupilPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Pupil access is required");
        }

        if (!AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }
    }

    private void verifyTeacherPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Teacher access is required");
        }

        if (!AccountRole.TEACHER.name().equals(principal.role())) {
            throw new AccessDeniedException("Teacher access is required");
        }
    }

    private void verifyTeacherOwnership(String teacherEmail, String classroomCode) {
        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(teacherEmail, classroomCode)) {
            throw new AccessDeniedException("You do not have access to this classroom");
        }
    }

    private PupilClassroomMembership getConfirmedPupilMembership(String pupilEmail) {
        return membershipRepository.findByAccountEmailAndMembershipConfirmedByTeacherTrue(normalizeEmail(pupilEmail))
                .orElseThrow(() -> new AccessDeniedException("Confirmed classroom membership is required"));
    }

    private Chapter getRequiredChapter(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
    }

    private PupilChapterBridgeProgression getOrCreateProgression(PupilClassroomMembership membership, Chapter chapter) {
        return progressionRepository.findByIdEmailAndIdChapterId(membership.getAccountEmail(), chapter.getId())
                .map(this::ensureNotebookInitialized)
                .orElseGet(() -> createDefaultProgression(membership, chapter));
    }

    private PupilChapterBridgeProgression ensureNotebookInitialized(PupilChapterBridgeProgression progression) {
        if (progression.getNotebookJson() != null && !progression.getNotebookJson().isBlank()) {
            return progression;
        }

        progression.setNotebookJson(notebookJsonService.defaultNotebookJson());
        return progressionRepository.save(progression);
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

    private ChapterNotebookResponse toResponse(Chapter chapter, PupilChapterBridgeProgression progression) {
        ChapterNotebookData notebookData = notebookJsonService.parse(progression.getNotebookJson());
        return new ChapterNotebookResponse(
                chapter.getId(),
                chapter.getChapterName(),
                notebookData.tips(),
                notebookData.ownNotes());
    }

    private String normalizeOwnNotes(String ownNotes) {
        return ownNotes == null ? "" : ownNotes;
    }

    private String normalizeEmail(String email) {
        return email.trim();
    }
}

