package com.nettdet.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.AuthMeResponse;
import com.nettdet.backend.dto.UpdateAvatarRequest;
import com.nettdet.backend.dto.UpdateMeRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.AccountService;

/**
 * REST controller for managing the authenticated user's profile.
 * <p>
 * This controller provides endpoints for users to retrieve their own profile information
 * and update their personal details or avatar configuration. It leverages the
 * {@link AccountService} to perform business logic and persistence.
 */
@RestController
@RequestMapping("/me")
public class MeController {

    private final AccountService accountService;

    /**
     * Constructs a new MeController with the required service.
     *
     * @param accountService the service for managing account-related operations
     */
    public MeController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Retrieves the profile information for the currently authenticated user.
     * <p>
     * Returns a detailed response containing the user's email, legal name, role, and other
     * relevant profile data. This endpoint is accessible to any authenticated user.
     *
     * @param principal the security principal of the authenticated user
     * @return a {@link ResponseEntity} containing the {@link AuthMeResponse}
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthMeResponse> getMe(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAuthenticatedProfile(principal.email()));
    }

    /**
     * Updates the personal profile details for the authenticated pupil.
     * <p>
     * Allows a pupil to modify their legal name or other profile attributes provided in the request body.
     * This endpoint is restricted to users with the 'PUPIL' authority to ensure data integrity
     * and role-specific validation.
     *
     * @param request the {@link UpdateMeRequest} containing the updated profile information
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the updated {@link AuthMeResponse}
     */
    @PutMapping
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<AuthMeResponse> updateMe(
            @RequestBody UpdateMeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(accountService.updateAuthenticatedProfile(principal, request));
    }

    /**
     * Updates the avatar configuration for the authenticated pupil.
     * <p>
     * Performs a partial update to the pupil's profile by changing their assigned avatar assets.
     * The system generates a new composite avatar image based on the selected assets. This
     * operation is restricted to users with the 'PUPIL' authority.
     *
     * @param request the {@link UpdateAvatarRequest} containing the new avatar asset identifiers
     * @param principal the security principal of the authenticated pupil
     * @return a {@link ResponseEntity} containing the updated {@link AuthMeResponse}
     */
    @PatchMapping
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<AuthMeResponse> updateAvatar(
            @RequestBody UpdateAvatarRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(accountService.updateAvatar(principal, request));
    }
}
