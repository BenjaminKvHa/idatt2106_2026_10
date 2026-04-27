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

@RestController
@Tag(name = "Tips", description = "Pupil chapter tips that are saved into the chapter notebook when received.")
@SecurityRequirement(name = "bearerAuth")
public class TipController {

    private final TipService tipService;

    public TipController(TipService tipService) {
        this.tipService = tipService;
    }

    @GetMapping("/pupil/chapters/{chapterId}/tip")
    @PreAuthorize("hasAuthority('PUPIL')")
    @Operation(summary = "Get chapter tip", description = "Returns one configured tip for the authenticated pupil's chapter. Newly received tips are saved to the pupil notebook without duplicates.")
    public ResponseEntity<ChapterTipResponse> getPupilTip(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(tipService.getPupilTip(chapterId, principal));
    }
}
