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

/**
 * REST controller for managing and retrieving pupil progression data.
 * <p>
 * This controller provides endpoints for pupils to track their progress through the learning chapters.
 * It tracks which chapters have been unlocked and the number of stars earned in each. The data is
 * used by the frontend to determine the unlock state of the curriculum and display achievement levels.
 * It integrates with {@link ProgressService} for data retrieval and calculation.
 */
@RestController
@RequestMapping("/pupil")
@Tag(name = "Pupil Progress", description = "Pupil-only progress endpoints used for chapter unlock state and star display.")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    /**
     * Constructs a new ProgressController with the required service.
     *
     * @param progressService the service responsible for pupil progress logic
     */
    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * Retrieves the chapter progress for the currently authenticated pupil.
     * <p>
     * Returns an ordered list of progress records for all chapters in the system. Each record
     * specifies whether the chapter is currently unlocked for the pupil and how many stars
     * they have earned through task completion. Access is restricted to users with the 'PUPIL' authority.
     *
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing a list of {@link PupilChapterProgressResponse} objects sorted by chapter ID
     */
    @GetMapping("/progress")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil chapter progress", description = "Returns the authenticated pupil's chapter progress list in chapter-id order, including current star count and whether each chapter is unlocked.")
    public ResponseEntity<List<PupilChapterProgressResponse>> getProgress(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(progressService.getPupilProgress(principal));
    }
}
