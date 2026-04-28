package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.dto.FileReferenceResponse;
import com.nettdet.backend.dto.TaskAdminResponse;
import com.nettdet.backend.service.TaskAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/tasks")
@Tag(name = "Task Admin", description = "Teacher-only endpoints for creating and managing tasks.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('TEACHER')")
public class TaskAdminController {

    private final TaskAdminService taskAdminService;

    public TaskAdminController(TaskAdminService taskAdminService) {
        this.taskAdminService = taskAdminService;
    }

    // --------------------------------------------------
    // FILE REFERENCE FROM MINIO
    // --------------------------------------------------

    @GetMapping("/files/reference")
    @Operation(summary = "Get or create file reference from MinIO",
               description = "Looks up a file in MinIO by object key and returns its file object ID. " +
                            "If the file exists in MinIO but not in the database, it creates a new file object entry. " +
                            "This allows referencing pre-uploaded files without knowing their IDs beforehand.")
    public ResponseEntity<FileReferenceResponse> getOrCreateFileReference(
            @RequestParam String objectKey,
            @RequestParam String contentType,
            @RequestParam String fileName) {
        FileReferenceResponse response = taskAdminService.getOrCreateFileReference(objectKey, contentType, fileName);
        return ResponseEntity.status(response.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    // --------------------------------------------------
    // TASK LISTING (per chapter, scoped to a classroom)
    // --------------------------------------------------

    @GetMapping("/fotografen")
    @Operation(summary = "List Fotografen tasks for a classroom",
               description = "Returns all Fotografen tasks scoped to the given classroom code. " +
                            "Caller must be a teacher of that classroom.")
    public ResponseEntity<List<TaskAdminResponse>> listFotografenTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listFotografenTasksForClassroom(principal.email(), classroomCode));
    }

    @GetMapping("/nyhetskvartalet")
    @Operation(summary = "List Nyhetskvartalet tasks for a classroom",
               description = "Returns all Nyhetskvartalet tasks scoped to the given classroom code. " +
                            "Caller must be a teacher of that classroom.")
    public ResponseEntity<List<TaskAdminResponse>> listNyhetskvartaletTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listNyhetskvartaletTasksForClassroom(principal.email(), classroomCode));
    }

    @GetMapping("/datasenteret")
    @Operation(summary = "List Datasenteret tasks for a classroom",
               description = "Returns all Datasenteret tasks scoped to the given classroom code. " +
                            "Caller must be a teacher of that classroom.")
    public ResponseEntity<List<TaskAdminResponse>> listDatasenteretTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listDatasenteretTasksForClassroom(principal.email(), classroomCode));
    }

    // --------------------------------------------------
    // TASK CREATION: FOTOGRAFEN
    // --------------------------------------------------

    @PostMapping("/fotografen")
    @Operation(summary = "Create Fotografen task",
               description = "Creates a new image verification task for the Fotografen chapter. " +
                            "Requires a fileId referencing an image stored in MinIO.")
    public ResponseEntity<TaskAdminResponse> createImageVerificationTask(
            @Valid @RequestBody CreateImageVerificationTaskRequest request) {
        TaskAdminResponse task = taskAdminService.createImageVerificationTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    // --------------------------------------------------
    // TASK CREATION: NYHETSKVARTALET
    // --------------------------------------------------

    @PostMapping("/nyhetskvartalet")
    @Operation(summary = "Create Nyhetskvartalet task",
               description = "Creates a new news verification task for the Nyhetskvartalet chapter. " +
                            "Requires two fileIds for comparison and a correctFileId indicating which is the credible source.")
    public ResponseEntity<TaskAdminResponse> createNewsVerificationTask(
            @Valid @RequestBody CreateNewsVerificationTaskRequest request) {
        TaskAdminResponse task = taskAdminService.createNewsVerificationTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    // --------------------------------------------------
    // TASK CREATION: DATASENTERET
    // --------------------------------------------------

    @PostMapping("/datasenteret/image")
    @Operation(summary = "Create Datasenteret image task",
               description = "Creates a new Datasenteret task using the image-verification format. " +
                            "Always associated with the Datasenteret chapter at HARD difficulty.")
    public ResponseEntity<TaskAdminResponse> createDatacenterImageTask(
            @Valid @RequestBody CreateDatacenterImageTaskRequest request) {
        TaskAdminResponse task = taskAdminService.createDatacenterImageTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PostMapping("/datasenteret/news")
    @Operation(summary = "Create Datasenteret news task",
               description = "Creates a new Datasenteret task using the news-verification format. " +
                            "Always associated with the Datasenteret chapter at HARD difficulty.")
    public ResponseEntity<TaskAdminResponse> createDatacenterNewsTask(
            @Valid @RequestBody CreateDatacenterNewsTaskRequest request) {
        TaskAdminResponse task = taskAdminService.createDatacenterNewsTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }
}
