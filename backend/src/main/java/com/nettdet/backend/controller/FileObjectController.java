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

@RestController
@RequestMapping("/objects")
@Tag(name = "File Objects", description = "Authenticated endpoints for uploading, reading, and deleting stored files with classroom-aware access rules.")
@SecurityRequirement(name = "bearerAuth")
public class FileObjectController {

    private final FileObjectAccessControlService fileObjectAccessControlService;

    public FileObjectController(FileObjectAccessControlService fileObjectAccessControlService) {
        this.fileObjectAccessControlService = fileObjectAccessControlService;
    }

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
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());

        return new ResponseEntity<>(storedFile.bytes(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/{fileObjectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete file object", description = "Deletes a stored file when the authenticated user is either the uploader or a teacher in the file's classroom.")
    public ResponseEntity<Void> delete(
            @PathVariable Long fileObjectId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        fileObjectAccessControlService.delete(fileObjectId, principal);
        return ResponseEntity.noContent().build();
    }

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
}
