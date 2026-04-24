package com.nettdet.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.RefreshToken;
import com.nettdet.backend.dto.AuthTokenResponse;
import com.nettdet.backend.dto.LoginRequest;
import com.nettdet.backend.dto.RegisterRequest;
import com.nettdet.backend.service.AccountService;
import com.nettdet.backend.service.RefreshTokenService;
import com.nettdet.backend.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for sign up, sign in, refresh, and logout.")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final RefreshTokenService refTokenService;
    private final AccountService accountService;
    private JwtUtil jwtUtil;

    public AuthController(RefreshTokenService refTokenService, JwtUtil jwtUtil, AccountService accountService) {
        this.refTokenService = refTokenService;
        this.jwtUtil = jwtUtil;
        this.accountService = accountService;
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Requires the current user to be authenticated. Rotates the refresh cookie and returns a new short-lived JWT access token.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AuthTokenResponse> grantRefreshToken(@CookieValue("refresh_token") String cookie) {

        logger.info("Refresh token request received");

        RefreshToken refToken = refTokenService.getStoredRefreshToken(cookie);
        Account account = refToken.getAccount();
        logger.debug("Refresh token resolved for account: {}", account.getEmail());

        ResponseCookie refCookie = refTokenService.rotateRefreshToken(refToken);
        String jwtToken = jwtUtil.generateToken(account);
        logger.info("Issued new access and refresh token for account: {}", account.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refCookie.toString())
                .body(new AuthTokenResponse(jwtToken));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Logout",
            description = "Requires the current user to be authenticated. Invalidates the refresh token cookie for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> logout(@CookieValue("refresh_token") String cookie) {
        logger.info("Logout request received");
        ResponseCookie expiredCookie = refTokenService.logout(cookie);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build();
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates a user with email and password, sets the refresh-token cookie, and returns a JWT access token.")
    public ResponseEntity<AuthTokenResponse> login(@RequestBody LoginRequest request) {
        Account logIn = accountService.login(request);
        String jwtToken = jwtUtil.generateToken(logIn);
        ResponseCookie refCookie = refTokenService.createAndStoreRefreshToken(logIn);

        logger.info("User logged in succesfully: {}", logIn.getEmail());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refCookie.toString())
                .body(new AuthTokenResponse(jwtToken));

    }

    @PostMapping("/signup")
    @Operation(
            summary = "Signup",
            description = "Registers a new pupil or teacher account, sets the refresh-token cookie, and returns a JWT access token.")
    public ResponseEntity<AuthTokenResponse> register(@RequestBody RegisterRequest request) {
        logger.info("Registration request received for email: {}", request.email());

        Account account = accountService.register(request);
        String jwtToken = jwtUtil.generateToken(account);
        ResponseCookie refCookie = refTokenService.createAndStoreRefreshToken(account);

        logger.info("Account registered and logged in: {}", account.getEmail());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refCookie.toString())
                .body(new AuthTokenResponse(jwtToken));
    }
}
