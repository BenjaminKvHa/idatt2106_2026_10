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

/**
 * REST controller for managing 'Weekly Mystery' (Ukens mysterium) submissions and approvals.
 * <p>
 * This controller handles the lifecycle of weekly mysteries, including pupil submissions
 * (files and comments), teacher moderation (approving or declining), and retrieval of
 * approved content for classroom-wide viewing. It enforces strict role-based access
 * control and classroom scoping via {@link WeeklyMysteryService}.
 */
@RestController
@Tag(name = "Weekly Mysteries", description = "Pupil submissions and teacher approval flow for Ukens mysterium.")
@SecurityRequirement(name = "bearerAuth")
public class WeeklyMysteryController {

    private final WeeklyMysteryService weeklyMysteryService;

    /**
     * Constructs a new WeeklyMysteryController with the required service.
     *
     * @param weeklyMysteryService the service responsible for weekly mystery logic and moderation
     */
    public WeeklyMysteryController(WeeklyMysteryService weeklyMysteryService) {
        this.weeklyMysteryService = weeklyMysteryService;
    }

    /**
     * Submits a new weekly mystery on behalf of the authenticated pupil.
     * <p>
     * Allows a pupil with confirmed membership in a classroom to upload an image or document
     * along with a descriptive comment. The submission is initially hidden from other pupils
     * and is only visible to teachers for moderation purposes.
     *
     * @param classroomCode the unique code of the classroom
     * @param file the multipart file being submitted as part of the mystery
     * @param comment a descriptive text provided by the pupil
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the {@link CreateWeeklyMysteryResponse}
     */
    @PostMapping("/pupil/classrooms/{classroomCode}/weekly-mysteries")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Submit weekly mystery", description = "Allows a confirmed pupil in the classroom to submit one file and a comment. The uploaded file starts as teacher-only until approved.")
    public ResponseEntity<CreateWeeklyMysteryResponse> submitMystery(
            @PathVariable String classroomCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam("comment") String comment,
            @RequestParam(value = "altText", required = false) String altText,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(weeklyMysteryService.submitMystery(classroomCode, file, comment, altText, principal));
    }

    /**
     * Retrieves all approved weekly mysteries for a specific classroom.
     * <p>
     * Returns a list of mysteries that have been vetted and approved by a teacher. This view
     * is available to all confirmed pupils within the classroom, allowing them to see shared
     * submissions in descending order of approval date.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing a list of {@link PupilWeeklyMysteryResponse}
     */
    @GetMapping("/pupil/classrooms/{classroomCode}/weekly-mysteries")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "List approved weekly mysteries", description = "Returns all approved weekly mysteries for a classroom where the authenticated pupil has confirmed membership, ordered by approvedAt descending.")
    public ResponseEntity<List<PupilWeeklyMysteryResponse>> getApprovedMysteries(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.getApprovedMysteries(classroomCode, principal));
    }

    /**
     * Lists all pending weekly mystery submissions for a teacher's moderation queue.
     * <p>
     * Provides teachers with a view of all submissions that require review. Each entry
     * contains the necessary information for the teacher to either accept or decline
     * the submission.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing a list of {@link TeacherPendingWeeklyMysteryResponse}
     */
    @GetMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/pending")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List pending weekly mysteries", description = "Returns pending weekly mystery submissions for a classroom the authenticated teacher belongs to.")
    public ResponseEntity<List<TeacherPendingWeeklyMysteryResponse>> getPendingMysteries(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.getPendingMysteries(classroomCode, principal));
    }

    /**
     * Lists all approved weekly mysteries within a classroom for administrative view.
     * <p>
     * Provides teachers with a historical view of all approved submissions, including the
     * legal names of the submitting pupils. This is used for record-keeping and classroom management.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing a list of {@link TeacherApprovedWeeklyMysteryResponse}
     */
    @GetMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/approved")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List approved weekly mysteries for teacher", description = "Returns approved weekly mysteries for a classroom the authenticated teacher belongs to, ordered by approvedAt descending and including the submitting pupil's legal name.")
    public ResponseEntity<List<TeacherApprovedWeeklyMysteryResponse>> getApprovedMysteriesForTeacher(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.getApprovedMysteriesForTeacher(classroomCode, principal));
    }

    /**
     * Approves a pending weekly mystery submission.
     * <p>
     * Sets the approval timestamp and updates the visibility of the associated file
     * from teacher-only to classroom-only, making it accessible to all pupils in the classroom.
     *
     * @param classroomCode the unique code of the classroom
     * @param mysteryId the unique identifier of the mystery submission
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} containing the updated {@link PupilWeeklyMysteryResponse}
     */
    @PutMapping("/teacher/classrooms/{classroomCode}/weekly-mysteries/{mysteryId}/accept")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Accept weekly mystery", description = "Approves a weekly mystery, records approvedAt when first approved, and changes the linked file access to classroom-only.")
    public ResponseEntity<PupilWeeklyMysteryResponse> acceptMystery(
            @PathVariable String classroomCode,
            @PathVariable Long mysteryId,
            @RequestParam(value = "altText", required = false) String altText,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(weeklyMysteryService.acceptMystery(classroomCode, mysteryId, altText, principal));
    }

    /**
     * Declines and removes a weekly mystery submission.
     * <p>
     * Permanently deletes the submission record and its associated file asset from storage.
     * This action is irreversible and is used by teachers to moderate inappropriate or
     * irrelevant content.
     *
     * @param classroomCode the unique code of the classroom
     * @param mysteryId the unique identifier of the mystery submission to decline
     * @param principal the security principal of the authenticated teacher
     * @return a {@link ResponseEntity} with no content indicating successful deletion
     */
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
