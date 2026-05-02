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
import jakarta.validation.Valid;

/**
 * REST controller for handling authentication and authorization requests.
 * Provides endpoints for user registration, login, token refreshing, and logout.
 * This controller integrates with {@link AccountService} for credential validation and account creation,
 * {@link RefreshTokenService} for secure session management via HTTP-only cookies,
 * and {@link JwtUtil} for generating short-lived access tokens.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for sign up, sign in, refresh, and logout.")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final RefreshTokenService refTokenService;
    private final AccountService accountService;
    private JwtUtil jwtUtil;

    /**
     * Constructs a new AuthController with necessary dependencies.
     *
     * @param refTokenService the service for managing refresh tokens
     * @param jwtUtil the utility for generating JWT access tokens
     * @param accountService the service for account-related business logic
     */
    public AuthController(RefreshTokenService refTokenService, JwtUtil jwtUtil, AccountService accountService) {
        this.refTokenService = refTokenService;
        this.jwtUtil = jwtUtil;
        this.accountService = accountService;
    }

    /**
     * Refreshes the JWT access token using a valid refresh token stored in an HTTP-only cookie.
     * This method implements token rotation, meaning the old refresh token is invalidated
     * and a new one is issued along with the new access token.
     *
     * @param cookie the refresh token extracted from the "refresh_token" cookie
     * @return a {@link ResponseEntity} containing the new {@link AuthTokenResponse} (access token)
     *         and a new rotated "refresh_token" cookie in the headers
     * @throws com.nettdet.backend.exception.TokenException if the token is expired or invalid (handled globally)
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Rotates the refresh cookie and returns a new short-lived JWT access token when the refresh cookie is valid.")
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

    /**
     * Logs out the user by invalidating the provided refresh token and clearing the cookie.
     * Requires the user to be authenticated.
     *
     * @param cookie the refresh token extracted from the "refresh_token" cookie
     * @return a {@link ResponseEntity} with an empty body and an expired "refresh_token" cookie header
     */
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

    /**
     * Authenticates a user with email and password.
     * Upon successful authentication, generates a short-lived access token and a long-lived refresh token.
     *
     * @param request the {@link LoginRequest} containing user credentials
     * @return a {@link ResponseEntity} containing the {@link AuthTokenResponse} (access token)
     *         and the "refresh_token" cookie in the headers
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid (handled by accountService/security)
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates a user with email and password, sets the refresh-token cookie, and returns a JWT access token.")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Account logIn = accountService.login(request);
        String jwtToken = jwtUtil.generateToken(logIn);
        ResponseCookie refCookie = refTokenService.createAndStoreRefreshToken(logIn);

        logger.info("User logged in succesfully: {}", logIn.getEmail());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refCookie.toString())
                .body(new AuthTokenResponse(jwtToken));

    }

    /**
     * Registers a new account (pupil or teacher) based on the provided request data.
     * Automatically logs in the user after successful registration by issuing tokens.
     *
     * @param request the {@link RegisterRequest} containing registration details (email, password, role, etc.)
     * @return a {@link ResponseEntity} containing the {@link AuthTokenResponse} (access token)
     *         and the "refresh_token" cookie in the headers
     */
    @PostMapping("/signup")
    @Operation(
            summary = "Signup",
            description = "Registers a new pupil or teacher account, sets the refresh-token cookie, and returns a JWT access token.")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
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
