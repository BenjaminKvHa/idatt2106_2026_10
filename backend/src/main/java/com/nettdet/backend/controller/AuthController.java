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
import com.nettdet.backend.dto.LoginRequest;
import com.nettdet.backend.dto.RegisterRequest;
import com.nettdet.backend.service.AccountService;
import com.nettdet.backend.service.RefreshTokenService;
import com.nettdet.backend.util.JwtUtil;


@RestController
@RequestMapping("/auth")
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
    public ResponseEntity<?> grantRefreshToken(@CookieValue("refresh_token") String cookie) {
        logger.info("Refresh token request received");

        RefreshToken refToken = refTokenService.getStoredRefreshToken(cookie);
        Account account = refToken.getAccount();
        logger.debug("Refresh token resolved for account: {}", account.getEmail());

        ResponseCookie refCookie = refTokenService.rotateRefreshToken(refToken);
        String jwtToken = jwtUtil.generateToken(account);
        logger.info("Issued new access and refresh token for account: {}", account.getEmail());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refCookie.toString()).body(jwtToken); //TODO: Map.of("token", jwtToken)
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(@CookieValue("refresh_token") String cookie) {
        logger.info("Logout request received");
        ResponseCookie expiredCookie = refTokenService.logout(cookie);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Account logIn = accountService.login(request);
        String jwtToken = jwtUtil.generateToken(logIn);
        ResponseCookie refCookie = refTokenService.createAndStoreRefreshToken(logIn);

        logger.info("User logged in succesfully: {}", logIn.getEmail());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refCookie.toString()).body(jwtToken); //TODO: Map.of("token", jwtToken)
    }

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        logger.info("Registration request received for email: {}", request.email());

        Account account = accountService.register(request);
        String jwtToken = jwtUtil.generateToken(account);
        ResponseCookie refCookie = refTokenService.createAndStoreRefreshToken(account);

        logger.info("Account registered and logged in: {}", account.getEmail());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refCookie.toString()).body(jwtToken); //TODO: Map.of("token", jwtToken)
    }

}
