package com.nettdet.backend.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.FileObjectAccessControlService;
import com.nettdet.backend.service.FileObjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing secure file objects.
 * <p>
 * This controller provides endpoints for uploading, retrieving, and deleting files stored within the system.
 * It strictly enforces classroom-aware access control rules via the {@link FileObjectAccessControlService},
 * ensuring that files are only accessible to authorized users based on their role and classroom membership.
 */
@RestController
@RequestMapping("/objects")
@Tag(name = "File Objects", description = "Authenticated endpoints for uploading, reading, and deleting stored files with classroom-aware access rules.")
@SecurityRequirement(name = "bearerAuth")
public class FileObjectController {

    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private final FileObjectAccessControlService fileObjectAccessControlService;

    /**
     * Constructs a new FileObjectController with the specified access control service.
     *
     * @param fileObjectAccessControlService the service responsible for enforcing file access and storage logic
     */
    public FileObjectController(FileObjectAccessControlService fileObjectAccessControlService) {
        this.fileObjectAccessControlService = fileObjectAccessControlService;
    }

    /**
     * Retrieves the content of a stored file object.
     * <p>
     * Access is granted only if the authenticated user meets the requirements defined by the file's
     * {@link ClassroomAccess} level. This includes checks for public availability, classroom membership
     * for pupils, and teacher-specific restrictions. Files are served with their original content type
     * and an inline content disposition for browser compatibility.
     *
     * @param fileObjectId the unique numeric identifier of the file object to retrieve
     * @param principal the security principal of the authenticated user requesting the file
     * @return a {@link ResponseEntity} containing the raw file bytes and appropriate HTTP headers
     * @throws com.nettdet.backend.exception.ResourceNotFoundException if the file object does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the user lacks sufficient permissions
     */
    @GetMapping("/{fileObjectId}")
    @Operation(summary = "Read file object", description = "Returns a stored file by numeric file object id if the authenticated user has access according to the file access level and classroom membership rules.")
    public ResponseEntity<byte[]> read(
            @PathVariable Long fileObjectId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        FileObjectService.StoredFile storedFile = fileObjectAccessControlService.read(fileObjectId, principal);

        String contentType = storedFile.contentType();
        String fileName = storedFile.fileObject().getFileName();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(
                isInlineSafeContentType(contentType)
                        ? ContentDisposition.inline().filename(fileName).build()
                        : ContentDisposition.attachment().filename(fileName).build());

        return new ResponseEntity<>(storedFile.bytes(), headers, HttpStatus.OK);
    }

    /**
     * Permanently deletes a file object from storage and the database.
     * <p>
     * Deletion is permitted if the authenticated user is either the original uploader of the file
     * or a teacher within the classroom to which the file belongs. This ensures that teachers
     * maintain administrative control over files shared within their classrooms.
     *
     * @param fileObjectId the unique numeric identifier of the file object to delete
     * @param principal the security principal of the authenticated user performing the deletion
     * @return a {@link ResponseEntity} with no content indicating successful deletion
     * @throws org.springframework.security.access.AccessDeniedException if the user is neither the owner nor an authorized teacher
     */
    @DeleteMapping("/{fileObjectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete file object", description = "Deletes a stored file when the authenticated user is either the uploader or a teacher in the file's classroom.")
    public ResponseEntity<Void> delete(
            @PathVariable Long fileObjectId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        fileObjectAccessControlService.delete(fileObjectId, principal);
        return ResponseEntity.noContent().build();
    }

    /**
     * Uploads a new file and creates a corresponding file object metadata record.
     * <p>
     * During upload, the user can specify an optional classroom and an access level (defaulting to PUBLIC).
     * The system validates that the uploader has the right to associate the file with the specified classroom.
     * Files marked as non-public will require membership in the specified classroom for future retrieval.
     *
     * @param file the multipart file content to be stored
     * @param classroomCode an optional code of the classroom to associate the file with
     * @param accessLevel the desired visibility level (e.g., PUBLIC, CLASSROOM_ONLY, TEACHER_ONLY)
     * @param principal the security principal of the authenticated user performing the upload
     * @return a {@link ResponseEntity} containing the ID of the newly created file object
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload file object", description = "Uploads a new file with an optional classroom code and access level. Non-public files require classroom membership, and teacher-only files require the authenticated user to be a teacher in that classroom.")
    public ResponseEntity<Long> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "classroomCode", required = false) String classroomCode,
            @RequestParam(value = "accessLevel", defaultValue = "PUBLIC") ClassroomAccess accessLevel,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        FileObject stored = fileObjectAccessControlService.upload(file, classroomCode, accessLevel, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored.getId());
    }

    private boolean isInlineSafeContentType(String contentType) {
        return PNG_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || JPEG_CONTENT_TYPE.equalsIgnoreCase(contentType);
    }
}
