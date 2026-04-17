package com.nettdet.backend.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

import jakarta.persistence.EntityNotFoundException;

@Service
public class FileObjectAccessControlService {

    private final FileObjectService fileObjectService;
    private final FileObjectRepository fileObjectRepository;
    private final AccountRepository accountRepository;
    private final ClassroomRepository classroomRepository;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    private final PupilClassroomMembershipRepository pupilMembershipRepository;

    public FileObjectAccessControlService(
            FileObjectService fileObjectService,
            FileObjectRepository fileObjectRepository,
            AccountRepository accountRepository,
            ClassroomRepository classroomRepository,
            AccountClassroomBridgeTeacherRepository teacherBridgeRepository,
            PupilClassroomMembershipRepository pupilMembershipRepository) {
        this.fileObjectService = fileObjectService;
        this.fileObjectRepository = fileObjectRepository;
        this.accountRepository = accountRepository;
        this.classroomRepository = classroomRepository;
        this.teacherBridgeRepository = teacherBridgeRepository;
        this.pupilMembershipRepository = pupilMembershipRepository;
    }

    @Transactional
    public FileObject upload(
            MultipartFile file,
            String classroomCode,
            ClassroomAccess accessLevel,
            JwtUserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (accessLevel == null) {
            accessLevel = ClassroomAccess.PUBLIC;
        }

        Account actor = resolveActor(principal);
        Classroom classroom = resolveClassroom(classroomCode);

        if (classroom == null && accessLevel != ClassroomAccess.PUBLIC) {
            throw new IllegalArgumentException("Classroom code is required for non-public files");
        }

        if (classroom != null) {
            ensureClassMembership(actor.getEmail(), classroomCode);

            if (accessLevel == ClassroomAccess.ONLY_TEACHERS_IN_CLASS
                    && !isTeacherInClass(actor.getEmail(), classroomCode)) {
                throw new AccessDeniedException("Only teachers in class can upload teacher-only files");
            }
        }

        String objectKey = generateObjectKey(file.getOriginalFilename());
        String fileName = normalizeFileName(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());

        try {
            return fileObjectService.upload(
                    objectKey,
                    file.getBytes(),
                    contentType,
                    actor,
                    classroom,
                    accessLevel,
                    fileName);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read uploaded file bytes");
        }
    }

    @Transactional(readOnly = true)
    public FileObjectService.StoredFile read(Long fileObjectId, JwtUserPrincipal principal) {
        FileObject fileObject = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new EntityNotFoundException("File object not found"));

        if (!canRead(fileObject, principal)) {
            throw new AccessDeniedException("You do not have access to this file");
        }

        return fileObjectService.read(fileObjectId);
    }

    @Transactional
    public void delete(Long fileObjectId, JwtUserPrincipal principal) {
        FileObject fileObject = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new EntityNotFoundException("File object not found"));

        String actorEmail = extractEmail(principal);
        boolean uploader = fileObject.getUploadedByAccount() != null
                && actorEmail.equals(fileObject.getUploadedByAccount().getEmail());

        boolean teacherInClass = false;
        if (fileObject.getClassroom() != null) {
            teacherInClass = isTeacherInClass(actorEmail, fileObject.getClassroom().getClassroomCode());
        }

        if (!uploader && !teacherInClass) {
            throw new AccessDeniedException("Only uploader or teacher in class can delete this file");
        }

        fileObjectService.delete(fileObjectId);
    }

    private boolean canRead(FileObject fileObject, JwtUserPrincipal principal) {
        ClassroomAccess accessLevel = fileObject.getAccessLevel();
        if (accessLevel == ClassroomAccess.PUBLIC) {
            return true;
        }

        String classroomCode = fileObject.getClassroom() != null
                ? fileObject.getClassroom().getClassroomCode()
                : null;
        if (classroomCode == null) {
            return false;
        }

        String actorEmail = extractEmail(principal);

        if (accessLevel == ClassroomAccess.ONLY_TEACHERS_IN_CLASS) {
            return isTeacherInClass(actorEmail, classroomCode);
        }

        return isTeacherInClass(actorEmail, classroomCode)
                || pupilMembershipRepository.existsByAccountEmailAndClassroomClassroomCode(actorEmail, classroomCode);
    }

    private Account resolveActor(JwtUserPrincipal principal) {
        String actorEmail = extractEmail(principal);
        return accountRepository.findById(actorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private Classroom resolveClassroom(String classroomCode) {
        if (classroomCode == null || classroomCode.isBlank()) {
            return null;
        }

        return classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));
    }

    private void ensureClassMembership(String accountEmail, String classroomCode) {
        if (isTeacherInClass(accountEmail, classroomCode)) {
            return;
        }

        if (pupilMembershipRepository.existsByAccountEmailAndClassroomClassroomCode(accountEmail, classroomCode)) {
            return;
        }

        throw new AccessDeniedException("You must belong to class to upload class files");
    }

    private boolean isTeacherInClass(String accountEmail, String classroomCode) {
        return teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(accountEmail, classroomCode);
    }

    private String extractEmail(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        return principal.email();
    }

    private String generateObjectKey(String originalFileName) {
        String safeFileName = normalizeFileName(originalFileName);
        return UUID.randomUUID() + "_" + safeFileName;
    }

    private String normalizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file.bin";
        }
        return originalFileName;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }

}
