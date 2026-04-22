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

@RestController
@RequestMapping("/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChapterResponse>> getAllChapters() {
        return ResponseEntity.ok(chapterService.getAllChapters());
    }

    @GetMapping("/{chapterId}/progression")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<ChapterProgressionResponse> getProgression(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getProgression(principal.email(), chapterId));
    }

    @GetMapping("/{chapterId}/fotografen")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<ImageVerificationResponse> getImageVerificationTask(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId,
            @RequestParam TaskDifficulty difficulty) {
        return ResponseEntity.ok(
            chapterService.fetchImageVerificationTask(principal.email(), chapterId, difficulty));
    }

    @PostMapping("/fotografen/tasks/{taskId}/validate")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<ValidationResult> validateImageVerificationAnswer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long taskId,
            @RequestBody ImageVerificationRequest request) {
        return ResponseEntity.ok(
            chapterService.validateAnswer(principal.email(), "FOTOGRAFEN", taskId, request));
    }

    @GetMapping("/{chapterId}/nyhetskvartalet")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<NewsVerificationResponse> getNewsVerificationTask(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long chapterId,
            @RequestParam TaskDifficulty difficulty) {
        return ResponseEntity.ok(
            chapterService.fetchNewsVerificationTask(principal.email(), chapterId, difficulty));
    }

    @PostMapping("/nyhetskvartalet/tasks/{taskId}/validate")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<ValidationResult> validateNewsVerificationAnswer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long taskId,
            @RequestBody NewsVerificationRequest request) {
        return ResponseEntity.ok(
            chapterService.validateAnswer(principal.email(), "NYHETSKVARTALET", taskId, request));
    }
}
