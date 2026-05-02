package com.nettdet.backend.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

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

/**
 * Service class responsible for controlling access to {@link FileObject} entities.
 * 
 * <p>Enforces classroom-level access policies for uploading, reading, and deleting files.
 * Access levels (e.g., PUBLIC, ONLY_TEACHERS_IN_CLASS) are evaluated against the 
 * principal's role and classroom membership.
 */
@Service
public class FileObjectAccessControlService {

    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String INVALID_IMAGE_MESSAGE = "Only PNG and JPEG images are allowed";

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

    /**
     * Uploads a file with a specific access level and optional classroom context.
     * 
     * <p>Verifies that the principal has the necessary permissions to upload 
     * to the target classroom at the specified access level.
     *
     * @param file          the multipart file to upload
     * @param classroomCode the optional classroom code (required if access is not PUBLIC)
     * @param accessLevel   the desired {@link ClassroomAccess} level
     * @param principal     the authenticated user principal
     * @return the created {@link FileObject}
     * @throws AccessDeniedException    if the principal lacks permission
     * @throws IllegalArgumentException if the file is empty or parameters are invalid
     */
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

        ValidatedImageUpload validatedImage = validateAndReadSupportedImage(file);
        String objectKey = generateObjectKey(validatedImage.fileName());

        return fileObjectService.upload(
                objectKey,
                validatedImage.bytes(),
                validatedImage.contentType(),
                actor,
                classroom,
                accessLevel,
                validatedImage.fileName());
    }

    /**
     * Specialized upload for weekly mystery task files.
     * 
     * <p>Only pupils with a confirmed membership in the target classroom can upload 
     * these files. The access level is forced to {@code ONLY_TEACHERS_IN_CLASS}.
     *
     * @param file          the multipart file to upload
     * @param classroomCode the code of the target classroom
     * @param principal     the authenticated pupil principal
     * @return the created {@link FileObject}
     * @throws AccessDeniedException    if the principal is not a confirmed pupil in the classroom
     * @throws IllegalArgumentException if parameters are invalid
     */
    @Transactional
    public FileObject uploadWeeklyMysteryFile(
            MultipartFile file,
            String classroomCode,
            JwtUserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (principal == null || principal.email() == null || principal.role() == null
                || !"PUPIL".equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }

        Classroom classroom = resolveClassroom(classroomCode);
        if (classroom == null) {
            throw new IllegalArgumentException("Classroom code is required for weekly mystery files");
        }

        Account actor = resolveActor(principal);
        pupilMembershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                        actor.getEmail(),
                        classroomCode)
                .orElseThrow(() -> new AccessDeniedException("Confirmed classroom membership is required"));

        ValidatedImageUpload validatedImage = validateAndReadSupportedImage(file);
        String objectKey = generateObjectKey(validatedImage.fileName());

        return fileObjectService.upload(
                objectKey,
                validatedImage.bytes(),
                validatedImage.contentType(),
                actor,
                classroom,
                ClassroomAccess.ONLY_TEACHERS_IN_CLASS,
                validatedImage.fileName());
    }

    /**
     * Reads a file object's metadata and content if the principal has access.
     * 
     * @param fileObjectId the ID of the file object
     * @param principal    the authenticated user principal
     * @return the {@link FileObjectService.StoredFile} including bytes and metadata
     * @throws AccessDeniedException   if access is denied
     * @throws EntityNotFoundException if the file object is not found
     */
    @Transactional(readOnly = true)
    public FileObjectService.StoredFile read(Long fileObjectId, JwtUserPrincipal principal) {
        FileObject fileObject = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new EntityNotFoundException("File object not found"));

        if (!canRead(fileObject, principal)) {
            throw new AccessDeniedException("You do not have access to this file");
        }

        return fileObjectService.read(fileObjectId);
    }

    /**
     * Deletes a file object if the principal is the uploader or an owner of the associated classroom.
     * 
     * @param fileObjectId the ID of the file object to delete
     * @param principal    the authenticated user principal
     * @throws AccessDeniedException   if the principal lacks deletion rights
     * @throws EntityNotFoundException if the file object is not found
     */
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
                || pupilMembershipRepository.findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                        actorEmail,
                        classroomCode)
                .isPresent();
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

    private ValidatedImageUpload validateAndReadSupportedImage(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read uploaded file bytes");
        }

        ensureReadableImage(bytes);
        String contentType = detectSupportedImageContentType(file.getContentType(), file.getOriginalFilename());
        String fileName = normalizeFileName(file.getOriginalFilename());
        return new ValidatedImageUpload(bytes, contentType, fileName);
    }

    private void ensureReadableImage(byte[] bytes) {
        try {
            if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new IllegalArgumentException(INVALID_IMAGE_MESSAGE);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(INVALID_IMAGE_MESSAGE);
        }
    }

    private String detectSupportedImageContentType(String contentType, String originalFileName) {
        if (PNG_CONTENT_TYPE.equalsIgnoreCase(contentType) || hasFileExtension(originalFileName, ".png")) {
            return PNG_CONTENT_TYPE;
        }

        if (JPEG_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)
                || hasFileExtension(originalFileName, ".jpg")
                || hasFileExtension(originalFileName, ".jpeg")) {
            return JPEG_CONTENT_TYPE;
        }

        throw new IllegalArgumentException(INVALID_IMAGE_MESSAGE);
    }

    private boolean hasFileExtension(String fileName, String extension) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        return fileName.toLowerCase().endsWith(extension);
    }

    private record ValidatedImageUpload(byte[] bytes, String contentType, String fileName) {
    }

}
