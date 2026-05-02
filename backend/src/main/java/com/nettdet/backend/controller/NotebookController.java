package com.nettdet.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.ChapterNotebookResponse;
import com.nettdet.backend.dto.UpdateChapterNotebookRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.NotebookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing chapter-specific notebooks for pupils.
 * <p>
 * This controller provides endpoints for pupils to retrieve and update their personal notes
 * for each learning chapter. It also allows teachers to view a pupil's notebook in a read-only
 * format to monitor progress. It integrates with {@link NotebookService} to handle JSON-based
 * note storage and tips cataloging.
 */
@RestController
@Tag(name = "Notebooks", description = "Chapter notebook endpoints for pupil note editing and teacher read-only notebook views.")
@SecurityRequirement(name = "bearerAuth")
public class NotebookController {

    private final NotebookService notebookService;

    /**
     * Constructs a new NotebookController with the required service.
     *
     * @param notebookService the service for managing notebook data and logic
     */
    public NotebookController(NotebookService notebookService) {
        this.notebookService = notebookService;
    }

    /**
     * Retrieves the authenticated pupil's notebook for a specific chapter.
     * <p>
     * Returns the current state of the pupil's notebook, including their personal notes
     * and a set of read-only tips relevant to the chapter. If no progression data exists yet,
     * the system may initialize a default notebook state. This endpoint is restricted
     * to users with the 'PUPIL' authority.
     *
     * @param chapterId the unique identifier of the chapter
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the {@link ChapterNotebookResponse}
     */
    @GetMapping("/pupil/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil notebook", description = "Returns the authenticated pupil's notebook for a chapter. If the progression row does not yet exist, the backend may create a default notebook for a confirmed classroom membership.")
    public ResponseEntity<ChapterNotebookResponse> getPupilNotebook(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.getPupilNotebook(chapterId, principal));
    }

    /**
     * Updates the authenticated pupil's personal notes for a chapter.
     * <p>
     * Allows a pupil to persist their custom notes for a given chapter. Only the editable
     * notes section is updated; the accompanying tips remain read-only. Access is restricted
     * to users with the 'PUPIL' authority.
     *
     * @param chapterId the unique identifier of the chapter
     * @param request the {@link UpdateChapterNotebookRequest} containing the updated note text
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the updated {@link ChapterNotebookResponse}
     */
    @PutMapping("/pupil/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Update pupil notebook", description = "Updates only the authenticated pupil's editable own-notes text for a chapter notebook. Read-only tips are not changed by this endpoint.")
    public ResponseEntity<ChapterNotebookResponse> updatePupilNotebook(
            @PathVariable Long chapterId,
            @RequestBody UpdateChapterNotebookRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.updatePupilNotebook(chapterId, request, principal));
    }

    /**
     * Retrieves a read-only view of a pupil's notebook for a teacher.
     * <p>
     * Provides teachers with a view into a specific pupil's notes and tips for a chapter.
     * The authenticated teacher must belong to the classroom where the pupil is a member.
     * This ensures that teachers only see data for pupils under their supervision.
     *
     * @param classroomCode the unique code of the classroom
     * @param pupilEmail the email of the pupil whose notebook is being requested
     * @param chapterId the unique identifier of the chapter
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the {@link ChapterNotebookResponse}
     * @throws org.springframework.security.access.AccessDeniedException if the teacher does not manage the classroom or pupil
     */
    @GetMapping("/teacher/classrooms/{classroomCode}/pupils/{pupilEmail}/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Get teacher notebook view", description = "Returns a read-only view of one pupil's chapter notebook. The authenticated teacher must belong to the requested classroom.")
    public ResponseEntity<ChapterNotebookResponse> getTeacherNotebook(
            @PathVariable String classroomCode,
            @PathVariable String pupilEmail,
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.getTeacherNotebook(classroomCode, pupilEmail, chapterId, principal));
    }
}
