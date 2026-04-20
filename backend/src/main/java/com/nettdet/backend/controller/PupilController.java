package com.nettdet.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.PupilProgressionResponse;
import com.nettdet.backend.dto.PupilRequest;
import com.nettdet.backend.dto.PupilResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.PupilService;

@RestController
@RequestMapping("/pupil")
public class PupilController {

    private final PupilService pupilService;

    public PupilController(PupilService pupilService) {
        this.pupilService = pupilService;
    }

    @GetMapping("/classrooms/{classroomCode}")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<PupilResponse> getPupilInformation(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(pupilService.getPupilInformation(principal.email(), classroomCode));
    }

    @GetMapping("/classrooms/{classroomCode}/progression")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<PupilProgressionResponse> getPupilProgression(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(pupilService.getPupilProgressionPerChapter(principal.email(), classroomCode));
    }

    @GetMapping("/classrooms/{classroomCode}/membership")
    @PreAuthorize("hasAuthority('PUPIL')")
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
    public ResponseEntity<PupilResponse> requestToJoinClassroom(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        PupilResponse response = pupilService.requestToJoinClassroom(
                new PupilRequest(principal.email(), classroomCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
