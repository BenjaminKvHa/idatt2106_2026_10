package com.nettdet.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.ChapterProgressionResponse;
import com.nettdet.backend.dto.ChapterResponse;
import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationResponse;
import com.nettdet.backend.dto.NewsVerificationRequest;
import com.nettdet.backend.dto.NewsVerificationResponse;
import com.nettdet.backend.dto.ValidationResult;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ChapterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/chapters")
@Tag(name = "Chapters", description = "Chapter and task endpoints for pupils, including task fetch, answer validation, and chapter progression.")
@SecurityRequirement(name = "bearerAuth")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List chapters", description = "Returns all available chapters with id and chapter name for the authenticated user.")
    public ResponseEntity<List<ChapterResponse>> getAllChapters() {
        return ResponseEntity.ok(chapterService.getAllChapters());
    }

    @GetMapping("/{chapterId}/progression")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get chapter progression", description = "Returns the authenticated pupil's current progression state for a single chapter, including current run counters and stored score data.")
    public ResponseEntity<ChapterProgressionResponse> getProgression(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getProgression(principal.email(), chapterId));
    }

    @GetMapping("/{chapterId}/fotografen")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get Fotografen task", description = "Returns one image-verification task for the Fotografen chapter for the authenticated pupil. The difficulty is selected with the query parameter.")
    public ResponseEntity<ImageVerificationResponse> getImageVerificationTask(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId,
            @RequestParam TaskDifficulty difficulty) {
        return ResponseEntity.ok(
            chapterService.fetchImageVerificationTask(principal.email(), chapterId, difficulty));
    }

    @PostMapping("/fotografen/tasks/{taskId}/validate")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Validate Fotografen answer", description = "Validates the authenticated pupil's answer for one Fotografen task and returns correctness, feedback, and updated run state.")
    public ResponseEntity<ValidationResult> validateImageVerificationAnswer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long taskId,
            @RequestBody ImageVerificationRequest request) {
        return ResponseEntity.ok(
            chapterService.validateAnswer(principal.email(), "FOTOGRAFEN", taskId, request));
    }

    @GetMapping("/{chapterId}/nyhetskvartalet")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get Nyhetskvartalet task", description = "Returns one news-verification task for the Nyhetskvartalet chapter for the authenticated pupil. The difficulty is selected with the query parameter.")
    public ResponseEntity<NewsVerificationResponse> getNewsVerificationTask(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId,
            @RequestParam TaskDifficulty difficulty) {
        return ResponseEntity.ok(
            chapterService.fetchNewsVerificationTask(principal.email(), chapterId, difficulty));
    }

    @PostMapping("/nyhetskvartalet/tasks/{taskId}/validate")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Validate Nyhetskvartalet answer", description = "Validates the authenticated pupil's answer for one Nyhetskvartalet task and returns correctness, feedback, and updated run state.")
    public ResponseEntity<ValidationResult> validateNewsVerificationAnswer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long taskId,
            @RequestBody NewsVerificationRequest request) {
        return ResponseEntity.ok(
            chapterService.validateAnswer(principal.email(), "NYHETSKVARTALET", taskId, request));
    }
}
