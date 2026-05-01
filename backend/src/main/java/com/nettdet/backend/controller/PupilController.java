package com.nettdet.backend.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.PupilClassroomMembershipResponse;
import com.nettdet.backend.dto.PupilProgressionResponse;
import com.nettdet.backend.dto.PupilRequest;
import com.nettdet.backend.dto.PupilResponse;
import com.nettdet.backend.dto.UpdatePupilClassroomMembershipRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.PupilService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing pupil-specific classroom interactions.
 * <p>
 * This controller provides endpoints for pupils to join classrooms, manage their membership
 * details (such as display usernames and avatars), and view their progression within
 * a specific classroom context. Access is restricted to users with the 'PUPIL' authority.
 */
@RestController
@RequestMapping("/pupil")
@Tag(name = "Pupil", description = "Pupil-only endpoints for joining classrooms, finishing membership setup, and reading pupil-specific classroom data.")
@SecurityRequirement(name = "bearerAuth")
public class PupilController {

    private final PupilService pupilService;

    /**
     * Constructs a new PupilController with the required service.
     *
     * @param pupilService the service responsible for pupil-related business logic
     */
    public PupilController(PupilService pupilService) {
        this.pupilService = pupilService;
    }

    /**
     * Retrieves detailed classroom information for the authenticated pupil.
     * <p>
     * Returns the pupil's membership details for a specific classroom, including their
     * chosen username, confirmation status, and an overview of their progression. This
     * endpoint is used to populate the pupil's view of a classroom they have joined.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the {@link PupilResponse}
     */
    @GetMapping("/classrooms/{classroomCode}")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil classroom information", description = "Returns the authenticated pupil's classroom membership details, including username, confirmation state, avatar file object id, and chapter progression.")
    public ResponseEntity<PupilResponse> getPupilInformation(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(pupilService.getPupilInformation(principal.email(), classroomCode));
    }

    /**
     * Retrieves the chapter-by-chapter progression for a specific classroom.
     * <p>
     * Returns a list of progression entries representing the pupil's performance and completion
     * status for each chapter within the context of the specified classroom. This provides
     * a granular view of their learning journey.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the {@link PupilProgressionResponse}
     */
    @GetMapping("/classrooms/{classroomCode}/progression")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil classroom progression", description = "Returns the authenticated pupil's chapter progression entries for one classroom membership.")
    public ResponseEntity<PupilProgressionResponse> getPupilProgression(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(pupilService.getPupilProgressionPerChapter(principal.email(), classroomCode));
    }

    /**
     * Checks if the authenticated pupil is a member of a specific classroom.
     * <p>
     * Verifies the existence of a membership record (either pending or confirmed) for the
     * pupil in the specified classroom. Returns a successful response if membership exists,
     * or a forbidden status if it does not.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} with status 200 (OK) if a member, or 403 (FORBIDDEN) otherwise
     */
    @GetMapping("/classrooms/{classroomCode}/membership")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Check pupil membership", description = "Checks whether the authenticated pupil has a membership row for the classroom. Returns 200 when present and 403 when not present.")
    public ResponseEntity<Void> checkMembership(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        boolean isMember = pupilService.checkPupilMembershipForClass(
                new PupilRequest(principal.email(), classroomCode));
        if (isMember) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Marks the first-time pupil intro sequence as completed.
     * <p>
     * This materializes the first chapter progression row if it does not already exist.
     *
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} with status 204 (NO_CONTENT)
     */
    @PostMapping("/intro/complete")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Complete first-time intro", description = "Creates the default chapter 1 progression row for the authenticated confirmed pupil if it does not already exist.")
    public ResponseEntity<Void> completeIntroSequence(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        pupilService.completeIntroSequence(principal.email());
        return ResponseEntity.noContent().build();
    }

    /**
     * Submits a request for the authenticated pupil to join a classroom.
     * <p>
     * Creates an initial membership record with a status of 'PENDING'. A teacher managing
     * the classroom must subsequently approve this request before the pupil gains full access
     * to the classroom's features and leaderboard.
     *
     * @param classroomCode the unique code of the classroom to join
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the initial {@link PupilResponse} for the pending membership
     */
    @PostMapping("/classrooms/{classroomCode}/join")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Request to join classroom", description = "Creates a classroom join request for the authenticated pupil. The membership starts unconfirmed and must later be accepted by a teacher.")
    public ResponseEntity<PupilResponse> requestToJoinClassroom(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        PupilResponse response = pupilService.requestToJoinClassroom(
                new PupilRequest(principal.email(), classroomCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the pupil's classroom-specific display settings.
     * <p>
     * Allows the pupil to set or change their display username and avatar configuration
     * for a specific classroom. The avatar selection string must follow the system's
     * predefined naming convention for asset composition.
     *
     * @param classroomCode the unique code of the classroom
     * @param request the {@link UpdatePupilClassroomMembershipRequest} containing the new username and avatar
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the updated {@link PupilClassroomMembershipResponse}
     */
    @PutMapping("/classrooms/{classroomCode}/membership")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Update pupil classroom membership", description = "Sets the authenticated pupil's display username and avatar selection for a classroom membership. The avatar selection must use the format `<person>_<hair>_<costume>`.")
    public ResponseEntity<PupilClassroomMembershipResponse> setClassroomMembership(
            @PathVariable String classroomCode,
            @Valid @RequestBody UpdatePupilClassroomMembershipRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        PupilClassroomMembershipResponse response = pupilService.updateClassroomMembership(
                principal.email(),
                classroomCode,
                request.username(),
                request.avatar());

        return ResponseEntity.ok(response);
    }
}
