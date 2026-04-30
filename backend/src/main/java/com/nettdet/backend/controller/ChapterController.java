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
import com.nettdet.backend.dto.DatacenterAnswerRequest;
import com.nettdet.backend.dto.DatacenterRunResponse;
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

/**
 * REST controller for managing chapter-related activities for pupils.
 * Provides endpoints for listing chapters, tracking pupil progression,
 * fetching chapter-specific tasks (Fotografen, Nyhetskvartalet, Datasenteret),
 * and validating task answers.
 * Access to most endpoints is restricted to users with the 'PUPIL' authority.
 */
@RestController
@RequestMapping("/chapters")
@Tag(name = "Chapters", description = "Chapter and task endpoints for pupils, including task fetch, answer validation, and chapter progression.")
@SecurityRequirement(name = "bearerAuth")
public class ChapterController {

    private final ChapterService chapterService;

    /**
     * Constructs a new ChapterController with the required service.
     *
     * @param chapterService the service for managing chapter and task business logic
     */
    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /**
     * Retrieves a list of all available chapters.
     *
     * @return a {@link ResponseEntity} containing a list of {@link ChapterResponse} objects
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List chapters", description = "Returns all available chapters with id and chapter name for the authenticated user.")
    public ResponseEntity<List<ChapterResponse>> getAllChapters() {
        return ResponseEntity.ok(chapterService.getAllChapters());
    }

    /**
     * Retrieves the current progression state for a specific chapter for the authenticated pupil.
     * Progression includes counters for tasks completed and current scores.
     *
     * @param principal the authenticated user's principal
     * @param chapterId the ID of the chapter to fetch progression for
     * @return a {@link ResponseEntity} containing the {@link ChapterProgressionResponse}
     */
    @GetMapping("/{chapterId}/progression")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get chapter progression", description = "Returns the authenticated pupil's current progression state for a single chapter, including current run counters and stored score data.")
    public ResponseEntity<ChapterProgressionResponse> getProgression(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getProgression(principal.email(), chapterId));
    }

    /**
     * Fetches a single image-verification task (Fotografen) for the specified chapter and difficulty.
     *
     * @param principal the authenticated user's principal
     * @param chapterId the ID of the chapter
     * @param difficulty the desired difficulty of the task
     * @return a {@link ResponseEntity} containing the {@link ImageVerificationResponse}
     */
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

    /**
     * Validates a pupil's answer for an image-verification task (Fotografen).
     *
     * @param principal the authenticated user's principal
     * @param taskId the ID of the task being validated
     * @param request the {@link ImageVerificationRequest} containing the pupil's answer
     * @return a {@link ResponseEntity} containing the {@link ValidationResult} with feedback and updated state
     */
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

    /**
     * Fetches a single news-verification task (Nyhetskvartalet) for the specified chapter and difficulty.
     *
     * @param principal the authenticated user's principal
     * @param chapterId the ID of the chapter
     * @param difficulty the desired difficulty of the task
     * @return a {@link ResponseEntity} containing the {@link NewsVerificationResponse}
     */
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

    /**
     * Validates a pupil's answer for a news-verification task (Nyhetskvartalet).
     *
     * @param principal the authenticated user's principal
     * @param taskId the ID of the task being validated
     * @param request the {@link NewsVerificationRequest} containing the pupil's answer
     * @return a {@link ResponseEntity} containing the {@link ValidationResult} with feedback and updated state
     */
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

    /**
     * Fetches a full 'Datasenteret' run for the authenticated pupil.
     * A run consists of a fixed sequence of hard tasks. Requesting a new run resets any existing run.
     *
     * @param principal the authenticated user's principal
     * @param chapterId the ID of the chapter
     * @return a {@link ResponseEntity} containing the {@link DatacenterRunResponse}
     */
    @GetMapping("/{chapterId}/datasenteret/run")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get Datasenteret run", description = "Returns the full Datasenteret final-boss run for the authenticated pupil: an ordered list of 5 HARD tasks (2 news-verification, 3 image-verification). Resets any abandoned in-progress run.")
    public ResponseEntity<DatacenterRunResponse> getDatacenterRun(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.fetchDatacenterRun(principal.email(), chapterId));
    }

    /**
     * Validates a pupil's answer for a specific task within a 'Datasenteret' run.
     * The run progress is updated upon validation.
     *
     * @param principal the authenticated user's principal
     * @param taskId the ID of the task being validated
     * @param request the {@link DatacenterAnswerRequest} containing the pupil's answer
     * @return a {@link ResponseEntity} containing the {@link ValidationResult}
     */
    @PostMapping("/datasenteret/tasks/{taskId}/validate")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Validate Datasenteret answer", description = "Validates the authenticated pupil's answer for one Datasenteret task. The request's format must match the served task's format. Run completes after 5 attempts; success requires all 5 correct.")
    public ResponseEntity<ValidationResult> validateDatacenterAnswer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long taskId,
            @RequestBody DatacenterAnswerRequest request) {
        return ResponseEntity.ok(
            chapterService.validateDatacenterAnswer(principal.email(), taskId, request));
    }
}
