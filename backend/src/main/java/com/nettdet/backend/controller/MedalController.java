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

/**
 * REST controller for managing pupil achievements and medals.
 * <p>
 * This controller provides endpoints for pupils to view their earned and potential medals.
 * Medals are awarded based on chapter completion and participation in various activities,
 * such as approved weekly mystery submissions. It integrates with {@link MedalService}
 * to determine the unlock status and metadata for each achievement.
 */
@RestController
@RequestMapping("/pupil")
@Tag(name = "Pupil Medals", description = "Pupil-only medal endpoints based on chapter completion and approved weekly mystery submissions.")
@SecurityRequirement(name = "bearerAuth")
public class MedalController {

    private final MedalService medalService;

    /**
     * Constructs a new MedalController with the required service.
     *
     * @param medalService the service responsible for medal logic and retrieval
     */
    public MedalController(MedalService medalService) {
        this.medalService = medalService;
    }

    /**
     * Retrieves all medals associated with the authenticated pupil.
     * <p>
     * Returns a complete list of medals, including those already unlocked and those still locked.
     * For unlocked medals, the specific achievement image is provided; for locked medals,
     * a generic "locked" placeholder image is used. This endpoint is restricted to users
     * with the 'PUPIL' authority.
     *
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing a list of {@link PupilMedalResponse} objects
     */
    @GetMapping("/medals")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get pupil medals", description = "Returns all medals for the authenticated pupil. Unlocked medals use their own medal image, while locked medals use the shared locked medal image.")
    public ResponseEntity<List<PupilMedalResponse>> getMedals(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(medalService.getPupilMedals(principal));
    }
}
