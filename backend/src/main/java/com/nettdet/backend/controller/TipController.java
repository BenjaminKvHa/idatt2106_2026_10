package com.nettdet.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.ChapterTipResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.TipService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for retrieving educational tips for pupils.
 * <p>
 * This controller provides endpoints for pupils to receive helpful tips and guidance
 * relevant to their current chapter. When a tip is retrieved, it is also automatically
 * persisted to the pupil's notebook for future reference, ensuring a seamless learning
 * experience. It integrates with {@link TipService} to manage tip distribution and
 * notebook synchronization.
 */
@RestController
@Tag(name = "Tips", description = "Pupil chapter tips that are saved into the chapter notebook when received.")
@SecurityRequirement(name = "bearerAuth")
public class TipController {

    private final TipService tipService;

    /**
     * Constructs a new TipController with the required service.
     *
     * @param tipService the service responsible for tip delivery and persistence logic
     */
    public TipController(TipService tipService) {
        this.tipService = tipService;
    }

    /**
     * Retrieves a relevant educational tip for the authenticated pupil in a specific chapter.
     * <p>
     * Fetches a single configured tip for the given chapter. Upon successful retrieval,
     * the system ensures that the tip is added to the pupil's notebook if it hasn't
     * been saved there already. Access is restricted to users with the 'PUPIL' authority.
     *
     * @param chapterId the unique identifier of the chapter for which a tip is requested
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the {@link ChapterTipResponse}
     */
    @GetMapping("/pupil/chapters/{chapterId}/tip")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get chapter tip", description = "Returns one configured tip for the authenticated pupil's chapter. Newly received tips are saved to the pupil notebook without duplicates.")
    public ResponseEntity<ChapterTipResponse> getPupilTip(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(tipService.getPupilTip(chapterId, principal));
    }
}
