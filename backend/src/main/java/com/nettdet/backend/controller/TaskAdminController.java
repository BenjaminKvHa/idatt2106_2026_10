package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/**
 * REST controller for administrative task management by teachers.
 * <p>
 * This controller provides comprehensive CRUD operations for learning tasks across different chapters
 * (Fotografen, Nyhetskvartalet, Datasenteret). It handles complex multipart requests that combine
 * JSON metadata with binary file uploads for task assets. Access is strictly restricted to users
 * with the 'TEACHER' authority, ensuring that only authorized educators can manage the curriculum.
 */
@RestController
@RequestMapping("/admin/tasks")
@Tag(name = "Task Admin", description = "Teacher-only endpoints for creating and managing tasks.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('TEACHER')")
public class TaskAdminController {

    private final TaskAdminService taskAdminService;

    /**
     * Constructs a new TaskAdminController with the required service.
     *
     * @param taskAdminService the service for performing administrative task operations
     */
    public TaskAdminController(TaskAdminService taskAdminService) {
        this.taskAdminService = taskAdminService;
    }

    // --------------------------------------------------
    // TASK LISTING (per chapter, scoped to a classroom)
    // --------------------------------------------------

    /**
     * Lists all 'Fotografen' (image-verification) tasks for a specific classroom.
     * <p>
     * Returns a detailed list of tasks associated with the specified classroom code. The authenticated
     * teacher must have administrative rights over the classroom to view its task catalog.
     *
     * @param principal the security principal of the authenticated teacher
     * @param classroomCode the unique code of the classroom to query
     * @return a {@link ResponseEntity} containing a list of {@link TaskAdminResponse} objects
     */
    @GetMapping("/fotografen")
    @Operation(summary = "List Fotografen tasks for a classroom")
    public ResponseEntity<List<TaskAdminResponse>> listFotografenTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listFotografenTasksForClassroom(principal.email(), classroomCode));
    }

    /**
     * Lists all 'Nyhetskvartalet' (news-verification) tasks for a specific classroom.
     * <p>
     * Returns a detailed list of news-related tasks for the given classroom. Authorization
     * is verified based on the teacher's association with the classroom.
     *
     * @param principal the security principal of the authenticated teacher
     * @param classroomCode the unique code of the classroom to query
     * @return a {@link ResponseEntity} containing a list of {@link TaskAdminResponse} objects
     */
    @GetMapping("/nyhetskvartalet")
    @Operation(summary = "List Nyhetskvartalet tasks for a classroom")
    public ResponseEntity<List<TaskAdminResponse>> listNyhetskvartaletTasks(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam String classroomCode) {
        return ResponseEntity.ok(
                taskAdminService.listNyhetskvartaletTasksForClassroom(principal.email(), classroomCode));
    }

    /**
     * Lists all 'Datasenteret' (mixed-format) tasks for a specific classroom.
     * <p>
     * Returns the collection of tasks used in the final Datasenteret run for the specified
     * classroom. This provides teachers with an overview of the difficulty and content of the final boss run.
     *
     * @param principal the security principal of the authenticated teacher
     * @param classroomCode the unique code of the classroom to query
     * @return a {@link ResponseEntity} containing a list of {@link TaskAdminResponse} objects
     */
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

    /**
     * Creates a new 'Fotografen' image-verification task.
     * <p>
     * This endpoint handles a multipart request containing task metadata (in JSON format) and
     * a single image file. The system processes the image, stores it securely, and links it
     * to the new task record.
     *
     * @param metadata the {@link CreateImageVerificationTaskRequest} containing task details
     * @param file the image file to be used in the task
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the created {@link TaskAdminResponse}
     */
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

    /**
     * Creates a new 'Nyhetskvartalet' news-verification task.
     * <p>
     * Handles a multipart request with JSON metadata and two image files (typically representing
     * two different news sources or snippets for comparison). Both files are required for
     * the creation of a valid news task.
     *
     * @param metadata the {@link CreateNewsVerificationTaskRequest} containing task details
     * @param file1 the first news-related image asset
     * @param file2 the second news-related image asset
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the created {@link TaskAdminResponse}
     */
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

    /**
     * Creates a new image-based task specifically for the 'Datasenteret' chapter.
     * <p>
     * Similar to the Fotografen creation, this endpoint specialized for the final chapter's
     * mixed format, requiring metadata and a single image asset.
     *
     * @param metadata the {@link CreateDatacenterImageTaskRequest} containing task details
     * @param file the image file asset
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the created {@link TaskAdminResponse}
     */
    @PostMapping(value = "/datasenteret/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create Datasenteret image-format task")
    public ResponseEntity<TaskAdminResponse> createDatacenterImageTask(
            @RequestPart("metadata") @Valid CreateDatacenterImageTaskRequest metadata,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        TaskAdminResponse task = taskAdminService.createDatacenterImageTask(metadata, file, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    /**
     * Creates a new news-based task specifically for the 'Datasenteret' chapter.
     * <p>
     * specialized for the final chapter's news format, requiring metadata and two image assets.
     *
     * @param metadata the {@link CreateDatacenterNewsTaskRequest} containing task details
     * @param file1 the first news asset
     * @param file2 the second news asset
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the created {@link TaskAdminResponse}
     */
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

    /**
     * Updates an existing 'Fotografen' task.
     * <p>
     * Allows for updating task metadata and optionally replacing the existing image asset.
     * If no new file is provided, the previous asset is retained.
     *
     * @param taskId the unique ID of the task to update
     * @param metadata the {@link UpdateImageVerificationTaskRequest} containing updated fields
     * @param file an optional new image file to replace the existing one
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the updated {@link TaskAdminResponse}
     */
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

    /**
     * Updates an existing 'Nyhetskvartalet' task.
     * <p>
     * metadata can be updated, and either or both image files can be replaced. Omitted files
     * will result in the existing assets being kept.
     *
     * @param taskId the unique ID of the task to update
     * @param metadata the {@link UpdateNewsVerificationTaskRequest} containing updated fields
     * @param file1 an optional replacement for the first news asset
     * @param file2 an optional replacement for the second news asset
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the updated {@link TaskAdminResponse}
     */
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

    /**
     * Updates an existing image-format task in the 'Datasenteret' chapter.
     * <p>
     * specialized update endpoint for Datasenteret tasks using the image-verification format.
     *
     * @param taskId the unique ID of the task to update
     * @param metadata updated task metadata
     * @param file an optional replacement image asset
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the updated {@link TaskAdminResponse}
     */
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

    /**
     * Updates an existing news-format task in the 'Datasenteret' chapter.
     * <p>
     * specialized update endpoint for Datasenteret tasks using the news-verification format.
     *
     * @param taskId the unique ID of the task to update
     * @param metadata updated task metadata
     * @param file1 an optional replacement for the first news asset
     * @param file2 an optional replacement for the second news asset
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the updated {@link TaskAdminResponse}
     */
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

    // --------------------------------------------------
    // TASK DELETION
    // --------------------------------------------------

    /**
     * Deletes a classroom-scoped task added by a teacher.
     * <p>
     * Removes the task record from the database. The authenticated teacher must belong to the
     * classroom the task is scoped to; global (classroom-less) tasks cannot be deleted via this
     * endpoint.
     * <p>
     * <b>Room for improvement:</b> Associated MinIO file objects are intentionally left in place,
     * which means storage will grow over time. A future iteration should clean up orphaned file
     * objects when their owning task is deleted.
     *
     * @param taskId the unique ID of the task to delete
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} with HTTP 204 No Content on success
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a classroom-scoped task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        taskAdminService.deleteTask(taskId, principal);
        return ResponseEntity.noContent().build();
    }
}
