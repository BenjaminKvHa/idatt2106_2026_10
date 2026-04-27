package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.PupilMedalResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.MedalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/pupil")
@Tag(name = "Pupil Medals", description = "Pupil-only medal endpoints based on chapter completion and approved weekly mystery submissions.")
@SecurityRequirement(name = "bearerAuth")
public class MedalController {

    private final MedalService medalService;

    public MedalController(MedalService medalService) {
        this.medalService = medalService;
    }

    @GetMapping("/medals")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil medals", description = "Returns all medals for the authenticated pupil. Unlocked medals use their own medal image, while locked medals use the shared locked medal image.")
    public ResponseEntity<List<PupilMedalResponse>> getMedals(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(medalService.getPupilMedals(principal));
    }
}
