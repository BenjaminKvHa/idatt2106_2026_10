package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nettdet.backend.dto.CreateWeeklyMysteryResponse;
import com.nettdet.backend.dto.PupilWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherApprovedWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherPendingWeeklyMysteryResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.WeeklyMysteryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Weekly Mysteries", description = "Pupil submissions and teacher approval flow for Ukens mysterium.")
@SecurityRequirement(name = "bearerAuth")
public class WeeklyMysteryController {

    private final WeeklyMysteryService weeklyMysteryService;

    public WeeklyMysteryController(WeeklyMysteryService weeklyMysteryService) {
        this.weeklyMysteryService = weeklyMysteryService;
    }

    @PostMapping("/pupil/classrooms/{classroomCode}/weekly-mysteries")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Submit weekly mystery", description = "Allows a confirmed pupil in the classroom to submit one file and a comment. The uploaded file starts as teacher-only until approved.")
    public ResponseEntity<CreateWeeklyMysteryResponse> submitMystery(
            @PathVariable String classroomCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam("comment") String comment,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(weeklyMysteryService.submitMystery(classroomCode, file, comment, principal));
    }

    @GetMapping("/pupil/classrooms/{classroomCode}/weekly-mysteries")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "List approved weekly mysteries", description = "Returns all approved weekly mysteries for a classroom where the authenticated pupil has confirmed membership, ordered by approvedAt descending.")
    public ResponseEntity<List<PupilWeeklyMysteryResponse>> getApprovedMysteries(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.getApprovedMysteries(classroomCode, principal));
    }

    @GetMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/pending")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List pending weekly mysteries", description = "Returns pending weekly mystery submissions for a classroom the authenticated teacher belongs to.")
    public ResponseEntity<List<TeacherPendingWeeklyMysteryResponse>> getPendingMysteries(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.getPendingMysteries(classroomCode, principal));
    }

    @GetMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/approved")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List approved weekly mysteries for teacher", description = "Returns approved weekly mysteries for a classroom the authenticated teacher belongs to, ordered by approvedAt descending and including the submitting pupil's legal name.")
    public ResponseEntity<List<TeacherApprovedWeeklyMysteryResponse>> getApprovedMysteriesForTeacher(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.getApprovedMysteriesForTeacher(classroomCode, principal));
    }

    @PutMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/{mysteryId}/accept")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Accept weekly mystery", description = "Approves a weekly mystery, records approvedAt when first approved, and changes the linked file access to classroom-only.")
    public ResponseEntity<PupilWeeklyMysteryResponse> acceptMystery(
            @PathVariable String classroomCode,
            @PathVariable Long mysteryId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.acceptMystery(classroomCode, mysteryId, principal));
    }

    @DeleteMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/{mysteryId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Decline weekly mystery", description = "Declines a weekly mystery by deleting both the request row and the uploaded file.")
    public ResponseEntity<Void> declineMystery(
            @PathVariable String classroomCode,
            @PathVariable Long mysteryId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        weeklyMysteryService.declineMystery(classroomCode, mysteryId, principal);
        return ResponseEntity.noContent().build();
    }
}
