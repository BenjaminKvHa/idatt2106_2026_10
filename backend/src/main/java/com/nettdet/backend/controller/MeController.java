package com.nettdet.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.AuthMeResponse;
import com.nettdet.backend.dto.UpdateMeRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.AccountService;

@RestController
@RequestMapping("/me")
public class MeController {

    private final AccountService accountService;

    public MeController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthMeResponse> getMe(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAuthenticatedProfile(principal.email()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PUPIL')")
    public ResponseEntity<AuthMeResponse> updateMe(
            @RequestBody UpdateMeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(accountService.updateAuthenticatedProfile(principal, request));
    }
}
