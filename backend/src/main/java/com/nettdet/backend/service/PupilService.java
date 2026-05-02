package com.nettdet.backend.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.PupilClassroomMembershipResponse;
import com.nettdet.backend.dto.PupilProgressionResponse;
import com.nettdet.backend.dto.PupilRequest;
import com.nettdet.backend.dto.PupilResponse;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Application service for pupil-side classroom membership operations.
 * <p>
 * A pupil's relationship to a classroom is modelled as a one-to-one
 * {@link PupilClassroomMembership} row with a {@code membership_confirmed_by_teacher}
 * flag — unconfirmed rows represent pending join requests. This service provides:
 * <ul>
 *     <li>read-only lookups of a pupil's membership and per-chapter progression;</li>
 *     <li>creation of pending join requests (idempotent: an existing row is returned
 *         instead of creating a duplicate);</li>
 *     <li>updating the pupil's classroom-scoped username and avatar.</li>
 * </ul>
 * Authorisation is the caller's responsibility; this service does not inspect the security
 * context. Avatar resolution is delegated to {@link AvatarService}.
 */
@Service
public class PupilService {

    private static final Logger logger = LoggerFactory.getLogger(PupilService.class);
    private static final long INTRO_CHAPTER_ID = 1L;

    private final PupilClassroomMembershipRepository pupilMembershipRepository;
    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final AccountRepository accountRepository;
    private final ClassroomRepository classroomRepository;
    private final ChapterRepository chapterRepository;
    private final AvatarService avatarService;
    private final NotebookJsonService notebookJsonService;

    /**
     * Constructs a {@code PupilService} with the dependencies required to read, create, and
     * update pupil classroom memberships.
     *
     * @param pupilMembershipRepository repository for pupil↔classroom membership rows
     * @param accountRepository         repository used to load the requesting pupil's account
     * @param classroomRepository       repository used to resolve target classrooms
     * @param avatarService             service used to look up or create the pupil's avatar
     */
    public PupilService(PupilClassroomMembershipRepository pupilMembershipRepository,
                        PupilChapterBridgeProgressionRepository progressionRepository,
                        AccountRepository accountRepository,
                        ClassroomRepository classroomRepository,
                        ChapterRepository chapterRepository,
                        NotebookJsonService notebookJsonService,
                        AvatarService avatarService) {
        this.pupilMembershipRepository = pupilMembershipRepository;
        this.progressionRepository = progressionRepository;
        this.accountRepository = accountRepository;
        this.classroomRepository = classroomRepository;
        this.chapterRepository = chapterRepository;
        this.notebookJsonService = notebookJsonService;
        this.avatarService = avatarService;
    }

    /**
     * Returns the pupil's identity within a specific classroom (email, classroom-scoped
     * username, etc.).
     *
     * @param email         the pupil's account email
     * @param classroomCode the classroom in which the membership is scoped
     * @return the membership projected as a {@link PupilResponse}
     * @throws EntityNotFoundException if no membership row exists for the pair
     */
    public PupilResponse getPupilInformation(String email, String classroomCode) {
        return pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(email, classroomCode)
               .map(PupilResponse::from)
               .orElseThrow(() -> {
                   logger.warn("Pupil membership not found for email {} in classroom {}", email, classroomCode);
                   return new EntityNotFoundException("Pupil membership not found");
               });
    }

    /**
     * Returns the pupil's per-chapter progression projected from the membership's chapter
     * progression rows.
     *
     * @param email         the pupil's account email
     * @param classroomCode the classroom in which the membership is scoped
     * @return the membership projected as a {@link PupilProgressionResponse}
     * @throws EntityNotFoundException if no membership row exists for the pair
     */
    public PupilProgressionResponse getPupilProgressionPerChapter(String email, String classroomCode) {
        return pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(email, classroomCode)
               .map(PupilProgressionResponse::from)
               .orElseThrow(() -> {
                   logger.warn("Pupil progression not found for email {} in classroom {}", email, classroomCode);
                   return new EntityNotFoundException("Pupil membership not found");
               });
    }


    /**
     * Reports whether the pupil has a teacher-confirmed membership in the requested classroom.
     * <p>
     * Returns {@code false} both when no membership row exists and when one exists but is still
     * pending teacher confirmation.
     *
     * @param pupilRequest carries the email + classroom code to check
     * @return {@code true} iff a confirmed membership row exists
     */
    public boolean checkPupilMembershipForClass(PupilRequest pupilRequest) {
        return pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(pupilRequest.email(), pupilRequest.classroomCode())
               .map(PupilClassroomMembership::isMembershipConfirmedByTeacher)
               .orElse(false);
    }

    /**
     * Completes the first-time intro by ensuring the first chapter progression row exists.
     * <p>
     * The operation is idempotent: existing chapter-one progression is left untouched.
     *
     * @param email the authenticated pupil's account email
     * @throws AccessDeniedException if the pupil has no confirmed classroom membership
     * @throws EntityNotFoundException if chapter 1 is missing
     */
    @Transactional
    public void completeIntroSequence(String email) {
        PupilClassroomMembership membership = pupilMembershipRepository
                .findByAccountEmailAndMembershipConfirmedByTeacherTrue(email)
                .orElseThrow(() -> new AccessDeniedException("Confirmed pupil membership is required"));

        if (progressionRepository.existsByIdEmailAndIdChapterId(email, INTRO_CHAPTER_ID)) {
            return;
        }

        Chapter chapter = chapterRepository.findById(INTRO_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));

        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(email, INTRO_CHAPTER_ID));
        progression.setPupilClassroomMembership(membership);
        progression.setChapter(chapter);
        progression.setSuccessfulRuns(0);
        progression.setHighscore(0);
        progression.setUnsuccessfulStreak(0);
        progression.setCurrentRunAttempts(0);
        progression.setCurrentRunCorrect(0);
        progression.setNotebookJson(notebookJsonService.defaultNotebookJson());

        progressionRepository.save(progression);
    }

    /**
     * Submits a join request for the pupil to a classroom, creating an unconfirmed
     * {@link PupilClassroomMembership} row.
     * <p>
     * The operation is idempotent: if a membership row already exists for the pair (confirmed
     * or pending), the existing row is returned instead of creating a duplicate.
     *
     * @param pupilRequest carries the pupil's email and the target classroom code
     * @return the (existing or newly created) membership as a {@link PupilResponse}
     * @throws EntityNotFoundException if the account or the classroom does not exist
     */
    @Transactional
    public PupilResponse requestToJoinClassroom(PupilRequest pupilRequest) {
        Account account = accountRepository.findById(pupilRequest.email())
               .orElseThrow(() -> {
                   logger.warn("Join request failed: account not found for email {}", pupilRequest.email());
                   return new EntityNotFoundException("Account not found");
               });

        Classroom classroom = classroomRepository.findById(pupilRequest.classroomCode())
               .orElseThrow(() -> {
                   logger.warn("Join request failed: classroom not found for code {}", pupilRequest.classroomCode());
                   return new EntityNotFoundException("Classroom not found");
               });

        Optional<PupilClassroomMembership> existing = pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(pupilRequest.email(), pupilRequest.classroomCode());
        if (existing.isPresent()) {
            logger.info("Join request: membership already exists for {} in {}", pupilRequest.email(), pupilRequest.classroomCode());
            return PupilResponse.from(existing.get());
        }

        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccount(account);
        membership.setClassroom(classroom);
        membership.setMembershipConfirmedByTeacher(false);

        PupilClassroomMembership saved = pupilMembershipRepository.save(membership);
        logger.info("Pupil {} requested to join classroom {}", pupilRequest.email(), pupilRequest.classroomCode());
        return PupilResponse.from(saved);
    }

    /**
     * Updates the classroom-scoped username and avatar on the pupil's membership.
     * <p>
     * The supplied avatar identifier is resolved (or created) by {@link AvatarService} and
     * the resulting {@link FileObject} is attached to the membership.
     *
     * @param email         the pupil's account email
     * @param classroomCode the classroom in which the membership is scoped
     * @param username      the new classroom-scoped display name; must not be blank
     * @param avatar        the avatar identifier passed through to {@link AvatarService}
     * @return the persisted membership projected as a {@link PupilClassroomMembershipResponse}
     * @throws IllegalArgumentException if {@code username} is null or blank
     * @throws EntityNotFoundException  if no membership row exists for the pair
     */
    @Transactional
    public PupilClassroomMembershipResponse updateClassroomMembership(
            String email,
            String classroomCode,
            String username,
            String avatar) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }

        PupilClassroomMembership membership = pupilMembershipRepository
                .findByAccountEmailAndClassroomClassroomCode(email, classroomCode)
                .orElseThrow(() -> {
                    logger.warn("Pupil membership not found for email {} in classroom {}", email, classroomCode);
                    return new EntityNotFoundException("Pupil membership not found");
                });

        membership.setUsername(username.trim());
        FileObject avatarFileObject = avatarService.getOrCreateAvatar(avatar);
        membership.setAvatarFileObject(avatarFileObject);

        PupilClassroomMembership savedMembership = pupilMembershipRepository.save(membership);
        return PupilClassroomMembershipResponse.from(savedMembership);
    }
}
