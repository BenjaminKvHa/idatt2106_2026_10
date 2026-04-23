package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.PupilChapterProgressResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ProgressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/pupil")
@Tag(name = "Pupil Progress", description = "Pupil-only progress endpoints used for chapter unlock state and star display.")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil chapter progress", description = "Returns the authenticated pupil's chapter progress list in chapter-id order, including current star count and whether each chapter is unlocked.")
    public ResponseEntity<List<PupilChapterProgressResponse>> getProgress(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(progressService.getPupilProgress(principal));
    }
}
