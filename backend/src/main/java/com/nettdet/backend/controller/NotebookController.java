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

@RestController
public class NotebookController {

    private final NotebookService notebookService;

    public NotebookController(NotebookService notebookService) {
        this.notebookService = notebookService;
    }

    @GetMapping("/pupil/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<ChapterNotebookResponse> getPupilNotebook(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.getPupilNotebook(chapterId, principal));
    }

    @PutMapping("/pupil/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<ChapterNotebookResponse> updatePupilNotebook(
            @PathVariable Long chapterId,
            @RequestBody UpdateChapterNotebookRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.updatePupilNotebook(chapterId, request, principal));
    }

    @GetMapping("/teacher/classrooms/{classroomCode}/pupils/{pupilEmail}/chapters/{chapterId}/notebook")
    @PreAuthorize("hasAuthority('TEACHER')")
    public ResponseEntity<ChapterNotebookResponse> getTeacherNotebook(
            @PathVariable String classroomCode,
            @PathVariable String pupilEmail,
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(notebookService.getTeacherNotebook(classroomCode, pupilEmail, chapterId, principal));
    }
}
