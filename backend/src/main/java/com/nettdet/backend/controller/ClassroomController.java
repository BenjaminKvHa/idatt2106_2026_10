package com.nettdet.backend.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.UpdateClassroomRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ClassroomService;

@RestController
@RequestMapping("/classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    public ResponseEntity<ClassroomResponse> createClassroom(
            @Valid @RequestBody CreateClassroomRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        ClassroomResponse response = classroomService.createClassroom(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{classroomCode}")
    @PreAuthorize("hasAuthority('TEACHER')")
    public ResponseEntity<ClassroomResponse> updateClassroom(
            @PathVariable String classroomCode,
            @Valid @RequestBody UpdateClassroomRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(classroomService.updateClassroom(classroomCode, request, principal));
    }

    @DeleteMapping("/{classroomCode}")
    @PreAuthorize("hasAuthority('TEACHER')")
    public ResponseEntity<Void> deleteClassroom(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        classroomService.deleteClassroom(classroomCode, principal);
        return ResponseEntity.noContent().build();
    }
}
