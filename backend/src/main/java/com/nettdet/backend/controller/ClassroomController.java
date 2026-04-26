package com.nettdet.backend.controller;

import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.AcceptedPupilMembershipResponse;
import com.nettdet.backend.dto.AcceptedClassroomPupilResponse;
import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.ClassroomTeacherResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.InviteTeacherRequest;
import com.nettdet.backend.dto.PendingPupilRequestResponse;
import com.nettdet.backend.dto.TeacherLeaveStatusResponse;
import com.nettdet.backend.dto.UpdateClassroomRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ClassroomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/classrooms")
@Tag(name = "Classrooms", description = "Teacher-managed endpoints for creating classrooms, managing teacher membership, and moderating pupil membership.")
@SecurityRequirement(name = "bearerAuth")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Create classroom", description = "Creates a classroom, generates a new 6-letter uppercase classroom code, and adds the authenticated teacher to the teacher bridge for that classroom.")
    public ResponseEntity<ClassroomResponse> createClassroom(
            @Valid @RequestBody CreateClassroomRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        ClassroomResponse response = classroomService.createClassroom(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/validate-code")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Validate classroom code", description = "Checks whether the submitted classroom code exists. This endpoint is intended for authenticated pupils before they submit a join request.")
    public ResponseEntity<ValidateClassroomCodeResponse> validateClassroomCode(
            @RequestBody ValidateClassroomCodeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.validateClassroomCode(request, principal));
    }

    @PostMapping("/{classroomCode}/teachers")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Invite teacher", description = "Adds another teacher account to a classroom the authenticated teacher already manages. The invited account must already exist and have the TEACHER role.")
    public ResponseEntity<Void> inviteTeacher(
            @PathVariable String classroomCode,
            @Valid @RequestBody InviteTeacherRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        classroomService.inviteTeacher(classroomCode, request, principal);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{classroomCode}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Update classroom", description = "Updates the name and description of a classroom the authenticated teacher belongs to.")
    public ResponseEntity<ClassroomResponse> updateClassroom(
            @PathVariable String classroomCode,
            @Valid @RequestBody UpdateClassroomRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.updateClassroom(classroomCode, request, principal));
    }

    @DeleteMapping("/{classroomCode}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Delete classroom", description = "Deletes a classroom the authenticated teacher belongs to. This removes the classroom and dependent bridge data.")
    public ResponseEntity<Void> deleteClassroom(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        classroomService.deleteClassroom(classroomCode, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classroomCode}/teachers/me/can-leave")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Check if teacher can leave classroom", description = "Returns whether the authenticated teacher can leave the classroom while still leaving at least one teacher attached to it.")
    public ResponseEntity<TeacherLeaveStatusResponse> canLeaveClassroom(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.getTeacherLeaveStatus(classroomCode, principal));
    }

    @DeleteMapping("/{classroomCode}/teachers/me")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Leave classroom", description = "Removes the authenticated teacher from the classroom if at least one other teacher remains.")
    public ResponseEntity<Void> leaveClassroom(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        classroomService.leaveClassroom(classroomCode, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{classroomCode}/pupils/me")
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<Void> leaveClassroomAsPupil(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        classroomService.leaveClassroomAsPupil(classroomCode, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classroomCode}/teachers")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List classroom teachers", description = "Returns all teachers attached to the classroom. The authenticated teacher must already belong to the classroom.")
    public ResponseEntity<List<ClassroomTeacherResponse>> getClassroomTeachers(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.getClassroomTeachers(classroomCode, principal));
    }

    @GetMapping("/{classroomCode}/pupils/pending")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List pending pupil requests", description = "Returns pupils who have requested to join the classroom but have not yet been accepted by a teacher.")
    public ResponseEntity<List<PendingPupilRequestResponse>> getPendingPupilRequests(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.getPendingPupilRequests(classroomCode, principal));
    }

    @GetMapping("/{classroomCode}/pupils")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "List accepted pupils", description = "Returns all accepted pupils in the classroom. Each entry includes the pupil email, legal name, display username, and the saved avatar object key when an avatar has been set.")
    public ResponseEntity<List<AcceptedClassroomPupilResponse>> getAcceptedPupils(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.getAcceptedPupils(classroomCode, principal));
    }

    @PutMapping("/{classroomCode}/pupils/{pupilEmail}/accept")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Accept pupil request", description = "Approves a pending pupil join request for the classroom. The pupil must already have a pending membership row.")
    public ResponseEntity<AcceptedPupilMembershipResponse> acceptPupilRequest(
            @PathVariable String classroomCode,
            @PathVariable String pupilEmail,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.acceptPupilRequest(classroomCode, pupilEmail, principal));
    }

    @DeleteMapping("/{classroomCode}/pupils/{pupilEmail}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Kick pupil", description = "Removes a pupil membership from the classroom, regardless of whether it is pending or already confirmed.")
    public ResponseEntity<Void> kickPupil(
            @PathVariable String classroomCode,
            @PathVariable String pupilEmail,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        classroomService.kickPupil(classroomCode, pupilEmail, principal);
        return ResponseEntity.noContent().build();
    }
}
