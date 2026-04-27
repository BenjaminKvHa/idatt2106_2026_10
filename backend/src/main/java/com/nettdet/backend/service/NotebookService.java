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

@Service
public class NotebookService {

    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final ChapterRepository chapterRepository;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    private final NotebookJsonService notebookJsonService;

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

    @Transactional
    public ChapterNotebookResponse getPupilNotebook(Long chapterId, JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);
        PupilClassroomMembership membership = getConfirmedPupilMembership(principal.email());
        Chapter chapter = getRequiredChapter(chapterId);
        PupilChapterBridgeProgression progression = getOrCreateProgression(membership, chapter);
        return toResponse(chapter, progression);
    }

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
