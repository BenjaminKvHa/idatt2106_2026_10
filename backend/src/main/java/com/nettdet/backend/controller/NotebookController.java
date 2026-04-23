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

@RestController
@Tag(name = "Notebooks", description = "Chapter notebook endpoints for pupil note editing and teacher read-only notebook views.")
@SecurityRequirement(name = "bearerAuth")
public class NotebookController {

    private final NotebookService notebookService;

    public NotebookController(NotebookService notebookService) {
        this.notebookService = notebookService;
    }

    @GetMapping("/pupil/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil notebook", description = "Returns the authenticated pupil's notebook for a chapter. If the progression row does not yet exist, the backend may create a default notebook for a confirmed classroom membership.")
    public ResponseEntity<ChapterNotebookResponse> getPupilNotebook(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.getPupilNotebook(chapterId, principal));
    }

    @PutMapping("/pupil/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Update pupil notebook", description = "Updates only the authenticated pupil's editable own-notes text for a chapter notebook. Read-only tips are not changed by this endpoint.")
    public ResponseEntity<ChapterNotebookResponse> updatePupilNotebook(
            @PathVariable Long chapterId,
            @RequestBody UpdateChapterNotebookRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.updatePupilNotebook(chapterId, request, principal));
    }

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
