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
import com.nettdet.backend.dto.TeacherPendingWeeklyMysteryResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.WeeklyMysteryRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

import jakarta.persistence.EntityNotFoundException;

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

    @Transactional
    public CreateWeeklyMysteryResponse submitMystery(String classroomCode,
                                                     MultipartFile file,
                                                     String comment,
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
        mystery.setApproved(false);

        WeeklyMystery savedMystery = weeklyMysteryRepository.saveAndFlush(mystery);
        notificationService.createNotification(
                normalizedClassroomCode,
                principal.email(),
                membership.getAccount().getLegalName() + " har sendt inn et nytt bidrag til Ukens mysterium.");

        return toCreateResponse(savedMystery);
    }

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

    @Transactional
    public PupilWeeklyMysteryResponse acceptMystery(String classroomCode,
                                                    Long mysteryId,
                                                    JwtUserPrincipal principal) {
        String normalizedClassroomCode = normalizeClassroomCode(classroomCode);
        verifyTeacherAccess(principal, normalizedClassroomCode);

        WeeklyMystery mystery = getRequiredMystery(mysteryId, normalizedClassroomCode);
        mystery.setApproved(true);
        if (mystery.getApprovedAt() == null) {
            mystery.setApprovedAt(LocalDateTime.now());
        }
        mystery.getFileObject().setAccessLevel(ClassroomAccess.ONLY_CLASS);
        fileObjectRepository.save(mystery.getFileObject());

        return toPupilResponse(weeklyMysteryRepository.save(mystery));
    }

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
                mystery.getCreatedAt(),
                mystery.getApprovedAt(),
                mystery.isApproved());
    }

    private PupilWeeklyMysteryResponse toPupilResponse(WeeklyMystery mystery) {
        return new PupilWeeklyMysteryResponse(
                mystery.getId(),
                mystery.getFileObject().getId(),
                mystery.getComment(),
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
}
