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

@RestController
@RequestMapping("/pupil")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<List<PupilChapterProgressResponse>> getProgress(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(progressService.getPupilProgress(principal));
    }
}
