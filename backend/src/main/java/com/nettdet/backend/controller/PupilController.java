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

@RestController
@RequestMapping("/pupil")
@Tag(name = "Pupil", description = "Pupil-only endpoints for joining classrooms, finishing membership setup, and reading pupil-specific classroom data.")
@SecurityRequirement(name = "bearerAuth")
public class PupilController {

    private final PupilService pupilService;

    public PupilController(PupilService pupilService) {
        this.pupilService = pupilService;
    }

    @GetMapping("/classrooms/{classroomCode}")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil classroom information", description = "Returns the authenticated pupil's classroom membership details, including username, confirmation state, avatar file object id, and chapter progression.")
    public ResponseEntity<PupilResponse> getPupilInformation(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(pupilService.getPupilInformation(principal.email(), classroomCode));
    }

    @GetMapping("/classrooms/{classroomCode}/progression")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil classroom progression", description = "Returns the authenticated pupil's chapter progression entries for one classroom membership.")
    public ResponseEntity<PupilProgressionResponse> getPupilProgression(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(pupilService.getPupilProgressionPerChapter(principal.email(), classroomCode));
    }

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
