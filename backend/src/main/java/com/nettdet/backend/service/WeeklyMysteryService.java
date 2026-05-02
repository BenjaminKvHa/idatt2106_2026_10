package com.nettdet.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.WeeklyMystery;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.dto.CreateWeeklyMysteryResponse;
import com.nettdet.backend.dto.PupilWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherApprovedWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherPendingWeeklyMysteryResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.WeeklyMysteryRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

import jakarta.persistence.EntityNotFoundException;

/**
 * Application service for the "Weekly Mystery" classroom feature.
 * <p>
 * The flow is moderation-style: pupils submit an image with a comment, the submission is
 * stored as <em>pending</em>, and a teacher must explicitly accept or decline it. While
 * pending, the underlying {@link FileObject} is uploaded with a restricted access level by
 * {@link FileObjectAccessControlService}; on acceptance, the file's access is widened to
 * {@link ClassroomAccess#ONLY_CLASS} so other pupils in the classroom can see it. On
 * rejection, both the {@link WeeklyMystery} row and its file object are deleted.
 * <p>
 * Pupil-side methods require a teacher-confirmed classroom membership; teacher-side methods
 * require the {@code TEACHER} role and a teacher↔classroom link. Submissions trigger a
 * notification to the teachers via {@link NotificationService} so the moderation queue
 * surfaces the new pending entry.
 */
@Service
public class WeeklyMysteryService {

    private final WeeklyMysteryRepository weeklyMysteryRepository;
    private final ClassroomRepository classroomRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;
    private final FileObjectAccessControlService fileObjectAccessControlService;
    private final NotificationService notificationService;

    /**
     * Constructs a {@code WeeklyMysteryService} with all collaborators required to persist
     * submissions, manage their files, authorise teacher actions, and emit notifications.
     *
     * @param weeklyMysteryRepository          repository for the {@code weekly_mystery} table
     * @param classroomRepository              repository used to validate classroom existence
     * @param membershipRepository             repository used to resolve confirmed pupil memberships
     * @param teacherBridgeRepository          repository used to verify teacher↔classroom ownership
     * @param fileObjectRepository             repository used to widen file access on acceptance
     * @param fileObjectService                low-level service used to delete files on rejection
     * @param fileObjectAccessControlService   service used to upload pending submission files
     * @param notificationService              service used to notify teachers of new submissions
     */
    public WeeklyMysteryService(WeeklyMysteryRepository weeklyMysteryRepository,
                                ClassroomRepository classroomRepository,
                                PupilClassroomMembershipRepository membershipRepository,
                                AccountClassroomBridgeTeacherRepository teacherBridgeRepository,
                                FileObjectRepository fileObjectRepository,
                                FileObjectService fileObjectService,
                                FileObjectAccessControlService fileObjectAccessControlService,
                                NotificationService notificationService) {
        this.weeklyMysteryRepository = weeklyMysteryRepository;
        this.classroomRepository = classroomRepository;
        this.membershipRepository = membershipRepository;
        this.teacherBridgeRepository = teacherBridgeRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
        this.fileObjectAccessControlService = fileObjectAccessControlService;
        this.notificationService = notificationService;
    }

    /**
     * Submits a new pending Weekly Mystery on behalf of the authenticated pupil.
     * <p>
     * Uploads the file via {@link FileObjectAccessControlService#uploadWeeklyMysteryFile}, persists
     * an unapproved {@link WeeklyMystery} row, and emits a teacher notification.
     *
     * @param classroomCode the classroom the submission belongs to
     * @param file          the image file being submitted
     * @param comment       the pupil's accompanying comment; must not be blank
     * @param principal     the authenticated pupil; must hold the {@code PUPIL} role and a confirmed
     *                      membership in {@code classroomCode}
     * @return the created submission projected as a {@link CreateWeeklyMysteryResponse}
     * @throws IllegalArgumentException if {@code classroomCode} or {@code comment} is blank
     * @throws AccessDeniedException    if the principal lacks a confirmed membership
     */
    @Transactional
    public CreateWeeklyMysteryResponse submitMystery(String classroomCode,
                                                      MultipartFile file,
                                                      String comment,
                                                      String altText,
                                                      JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        String normalizedComment = normalizeComment(comment);
        verifyPupilPrincipal(principal);

        PupilClassroomMembership membership = getConfirmedMembership(principal.email(), normalizedClassroomCode);
        FileObject fileObject = fileObjectAccessControlService.uploadWeeklyMysteryFile(
                file,
                normalizedClassroomCode,
                principal);

        WeeklyMystery mystery = new WeeklyMystery();
        mystery.setClassroom(membership.getClassroom());
        mystery.setPupilClassroomMembership(membership);
        mystery.setFileObject(fileObject);
        mystery.setComment(normalizedComment);
        mystery.setAltText(blankToNull(altText));
        mystery.setApproved(false);

        WeeklyMystery savedMystery = weeklyMysteryRepository.saveAndFlush(mystery);
        notificationService.createNotification(
                normalizedClassroomCode,
                principal.email(),
                membership.getAccount().getLegalName() + " har sendt inn et nytt bidrag til Ukens mysterium.");

        return toCreateResponse(savedMystery);
    }

    /**
     * Returns the classroom's accepted Weekly Mysteries for pupils, ordered by approval time
     * (newest first), with creation time as a tiebreaker.
     *
     * @param classroomCode the classroom to read from
     * @param principal     the authenticated pupil; must have a confirmed membership in the classroom
     * @return the list of approved submissions
     * @throws AccessDeniedException    if the principal lacks a confirmed membership
     * @throws IllegalArgumentException if {@code classroomCode} is blank
     */
    @Transactional(readOnly = true)
    public List<PupilWeeklyMysteryResponse> getApprovedMysteries(String classroomCode,
                                                                 JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyPupilPrincipal(principal);
        getConfirmedMembership(principal.email(), normalizedClassroomCode);

        return weeklyMysteryRepository
                .findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc(normalizedClassroomCode)
                .stream()
                .map(this::toPupilResponse)
                .toList();
    }

    /**
     * Returns the classroom's pending (unapproved) Weekly Mysteries for the moderating teacher,
     * newest first.
     *
     * @param classroomCode the classroom to read from
     * @param principal     the authenticated teacher; must be linked to the classroom
     * @return the moderation queue of pending submissions
     * @throws AccessDeniedException    if the principal is not a teacher of the classroom
     * @throws IllegalArgumentException if {@code classroomCode} is blank
     * @throws EntityNotFoundException  if the classroom does not exist
     */
    @Transactional(readOnly = true)
    public List<TeacherPendingWeeklyMysteryResponse> getPendingMysteries(String classroomCode,
                                                                         JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        return weeklyMysteryRepository
                .findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc(normalizedClassroomCode)
                .stream()
                .map(this::toTeacherPendingResponse)
                .toList();
    }

    /**
     * Returns the classroom's accepted Weekly Mysteries for the teacher view, ordered by
     * approval time (newest first), with creation time as a tiebreaker.
     * <p>
     * Differs from {@link #getApprovedMysteries(String, JwtUserPrincipal)} in that the teacher
     * projection includes the submitting pupil's name.
     *
     * @param classroomCode the classroom to read from
     * @param principal     the authenticated teacher; must be linked to the classroom
     * @return the list of approved submissions in teacher form
     * @throws AccessDeniedException    if the principal is not a teacher of the classroom
     * @throws IllegalArgumentException if {@code classroomCode} is blank
     * @throws EntityNotFoundException  if the classroom does not exist
     */
    @Transactional(readOnly = true)
    public List<TeacherApprovedWeeklyMysteryResponse> getApprovedMysteriesForTeacher(String classroomCode,
                                                                                     JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        return weeklyMysteryRepository
                .findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc(normalizedClassroomCode)
                .stream()
                .map(this::toTeacherApprovedResponse)
                .toList();
    }

    /**
     * Accepts a pending Weekly Mystery, making it visible to the rest of the classroom.
     * <p>
     * Sets {@code approved = true}, fills in {@code approvedAt} if not already set, and widens
     * the underlying file's access level to {@link ClassroomAccess#ONLY_CLASS}.
     *
     * @param classroomCode the classroom that owns the submission
     * @param mysteryId     id of the submission to accept
     * @param principal     the authenticated teacher; must be linked to the classroom
     * @return the accepted submission projected as a {@link PupilWeeklyMysteryResponse}
     * @throws AccessDeniedException    if the principal is not a teacher of the classroom
     * @throws IllegalArgumentException if {@code classroomCode} or {@code mysteryId} is invalid
     * @throws EntityNotFoundException  if the submission does not exist in the classroom
     */
    @Transactional
    public PupilWeeklyMysteryResponse acceptMystery(String classroomCode,
                                                     Long mysteryId,
                                                     String altText,
                                                     JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        WeeklyMystery mystery = getRequiredMystery(mysteryId, normalizedClassroomCode);
        mystery.setApproved(true);
        if (mystery.getApprovedAt() == null) {
            mystery.setApprovedAt(LocalDateTime.now());
        }
        if (altText != null && !altText.isBlank()) {
            mystery.setAltText(altText.trim());
        }
        mystery.getFileObject().setAccessLevel(ClassroomAccess.ONLY_CLASS);
        fileObjectRepository.save(mystery.getFileObject());

        return toPupilResponse(weeklyMysteryRepository.save(mystery));
    }

    /**
     * Rejects a pending Weekly Mystery and deletes both the submission and its underlying file.
     * <p>
     * The {@link WeeklyMystery} row is removed first (and the transaction flushed) so the
     * subsequent {@link FileObjectService#delete} call has no foreign-key dependency.
     *
     * @param classroomCode the classroom that owns the submission
     * @param mysteryId     id of the submission to reject
     * @param principal     the authenticated teacher; must be linked to the classroom
     * @throws AccessDeniedException    if the principal is not a teacher of the classroom
     * @throws IllegalArgumentException if {@code classroomCode} or {@code mysteryId} is invalid
     * @throws EntityNotFoundException  if the submission does not exist in the classroom
     */
    @Transactional
    public void declineMystery(String classroomCode,
                               Long mysteryId,
                               JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        WeeklyMystery mystery = getRequiredMystery(mysteryId, normalizedClassroomCode);
        Long fileObjectId = mystery.getFileObject().getId();
        weeklyMysteryRepository.delete(mystery);
        weeklyMysteryRepository.flush();
        fileObjectService.delete(fileObjectId);
    }

    private void verifyTeacherAccess(JwtUserPrincipal principal, String classroomCode) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);

        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(principal.email(), classroomCode)) {
            throw new AccessDeniedException("You do not have access to this classroom");
        }
    }

    private void verifyTeacherPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null
                || !AccountRole.TEACHER.name().equals(principal.role())) {
            throw new AccessDeniedException("Teacher access is required");
        }
    }

    private void verifyPupilPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null
                || !AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }
    }

    private PupilClassroomMembership getConfirmedMembership(String pupilEmail, String classroomCode) {
        return membershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                        pupilEmail,
                        classroomCode)
                .orElseThrow(() -> new AccessDeniedException("Confirmed classroom membership is required"));
    }

    private Classroom getRequiredClassroom(String classroomCode) {
        return classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));
    }

    private WeeklyMystery getRequiredMystery(Long mysteryId, String classroomCode) {
        if (mysteryId == null) {
            throw new IllegalArgumentException("Weekly mystery id is required");
        }

        return weeklyMysteryRepository.findByIdAndClassroomClassroomCode(mysteryId, classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Weekly mystery not found"));
    }

    private CreateWeeklyMysteryResponse toCreateResponse(WeeklyMystery mystery) {
        return new CreateWeeklyMysteryResponse(
                mystery.getId(),
                mystery.getFileObject().getId(),
                mystery.getComment(),
                mystery.getAltText(),
                mystery.getCreatedAt(),
                mystery.getApprovedAt(),
                mystery.isApproved());
    }

    private PupilWeeklyMysteryResponse toPupilResponse(WeeklyMystery mystery) {
        return new PupilWeeklyMysteryResponse(
                mystery.getId(),
                mystery.getFileObject().getId(),
                mystery.getComment(),
                mystery.getAltText(),
                mystery.getCreatedAt(),
                mystery.getApprovedAt());
    }

    private TeacherPendingWeeklyMysteryResponse toTeacherPendingResponse(WeeklyMystery mystery) {
        return new TeacherPendingWeeklyMysteryResponse(
                mystery.getId(),
                mystery.getFileObject().getId(),
                mystery.getPupilClassroomMembership().getAccountEmail(),
                mystery.getPupilClassroomMembership().getAccount().getLegalName(),
                mystery.getComment(),
                mystery.getAltText(),
                mystery.getCreatedAt(),
                mystery.getApprovedAt());
    }

    private TeacherApprovedWeeklyMysteryResponse toTeacherApprovedResponse(WeeklyMystery mystery) {
        return new TeacherApprovedWeeklyMysteryResponse(
                mystery.getId(),
                mystery.getFileObject().getId(),
                mystery.getPupilClassroomMembership().getAccount().getLegalName(),
                mystery.getComment(),
                mystery.getAltText(),
                mystery.getCreatedAt(),
                mystery.getApprovedAt());
    }

    private String normalizeClassroomCode(String classroomCode) {
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new IllegalArgumentException("Classroom code is required");
        }

        return classroomCode.trim();
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment is required");
        }

        return comment.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
