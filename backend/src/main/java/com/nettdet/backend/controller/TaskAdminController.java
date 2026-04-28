package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.dto.TaskAdminResponse;
import com.nettdet.backend.dto.UpdateImageVerificationTaskRequest;
import com.nettdet.backend.dto.UpdateNewsVerificationTaskRequest;
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
    // TASK LISTING (per chapter, scoped to a classroom)
    // --------------------------------------------------

    @GetMapping("/fotografen")
    @Operation(summary = "List Fotografen tasks for a classroom")
    public ResponseEntity<List<TaskAdminResponse>> listFotografenTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listFotografenTasksForClassroom(principal.email(), classroomCode));
    }

    @GetMapping("/nyhetskvartalet")
    @Operation(summary = "List Nyhetskvartalet tasks for a classroom")
    public ResponseEntity<List<TaskAdminResponse>> listNyhetskvartaletTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listNyhetskvartaletTasksForClassroom(principal.email(), classroomCode));
    }

    @GetMapping("/datasenteret")
    @Operation(summary = "List Datasenteret tasks for a classroom")
    public ResponseEntity<List<TaskAdminResponse>> listDatasenteretTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listDatasenteretTasksForClassroom(principal.email(), classroomCode));
    }

    // --------------------------------------------------
    // TASK CREATION
    // --------------------------------------------------

    @PostMapping(value = "/fotografen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create Fotografen task",
               description = "Creates a Fotografen task. Send `metadata` (JSON) and `file` (image) as multipart parts.")
    public ResponseEntity<TaskAdminResponse> createImageVerificationTask(
            @RequestPart("metadata") @Valid CreateImageVerificationTaskRequest metadata,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        TaskAdminResponse task = taskAdminService.createImageVerificationTask(metadata, file, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PostMapping(value = "/nyhetskvartalet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create Nyhetskvartalet task",
               description = "Creates a Nyhetskvartalet task. Send `metadata` (JSON), `file1`, and `file2` as multipart parts.")
    public ResponseEntity<TaskAdminResponse> createNewsVerificationTask(
            @RequestPart("metadata") @Valid CreateNewsVerificationTaskRequest metadata,
            @RequestPart("file1") MultipartFile file1,
            @RequestPart("file2") MultipartFile file2,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        TaskAdminResponse task = taskAdminService.createNewsVerificationTask(metadata, file1, file2, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PostMapping(value = "/datasenteret/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create Datasenteret image-format task")
    public ResponseEntity<TaskAdminResponse> createDatacenterImageTask(
            @RequestPart("metadata") @Valid CreateDatacenterImageTaskRequest metadata,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        TaskAdminResponse task = taskAdminService.createDatacenterImageTask(metadata, file, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PostMapping(value = "/datasenteret/news", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create Datasenteret news-format task")
    public ResponseEntity<TaskAdminResponse> createDatacenterNewsTask(
            @RequestPart("metadata") @Valid CreateDatacenterNewsTaskRequest metadata,
            @RequestPart("file1") MultipartFile file1,
            @RequestPart("file2") MultipartFile file2,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        TaskAdminResponse task = taskAdminService.createDatacenterNewsTask(metadata, file1, file2, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    // --------------------------------------------------
    // TASK UPDATE
    // --------------------------------------------------

    @PatchMapping(value = "/fotografen/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update Fotografen task",
               description = "Updates a Fotografen task. Send `metadata` (JSON); send `file` to replace the image, omit to keep existing.")
    public ResponseEntity<TaskAdminResponse> updateImageVerificationTask(
            @PathVariable Long taskId,
            @RequestPart("metadata") @Valid UpdateImageVerificationTaskRequest metadata,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(
                taskAdminService.updateImageVerificationTask(taskId, metadata, file, principal));
    }

    @PatchMapping(value = "/nyhetskvartalet/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update Nyhetskvartalet task",
               description = "Updates a Nyhetskvartalet task. Send `file1` and/or `file2` to replace either image; omit to keep existing.")
    public ResponseEntity<TaskAdminResponse> updateNewsVerificationTask(
            @PathVariable Long taskId,
            @RequestPart("metadata") @Valid UpdateNewsVerificationTaskRequest metadata,
            @RequestPart(value = "file1", required = false) MultipartFile file1,
            @RequestPart(value = "file2", required = false) MultipartFile file2,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(
                taskAdminService.updateNewsVerificationTask(taskId, metadata, file1, file2, principal));
    }

    @PatchMapping(value = "/datasenteret/image/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update Datasenteret image-format task")
    public ResponseEntity<TaskAdminResponse> updateDatacenterImageTask(
            @PathVariable Long taskId,
            @RequestPart("metadata") @Valid UpdateImageVerificationTaskRequest metadata,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(
                taskAdminService.updateDatacenterImageTask(taskId, metadata, file, principal));
    }

    @PatchMapping(value = "/datasenteret/news/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update Datasenteret news-format task")
    public ResponseEntity<TaskAdminResponse> updateDatacenterNewsTask(
            @PathVariable Long taskId,
            @RequestPart("metadata") @Valid UpdateNewsVerificationTaskRequest metadata,
            @RequestPart(value = "file1", required = false) MultipartFile file1,
            @RequestPart(value = "file2", required = false) MultipartFile file2,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(
                taskAdminService.updateDatacenterNewsTask(taskId, metadata, file1, file2, principal));
    }
}
