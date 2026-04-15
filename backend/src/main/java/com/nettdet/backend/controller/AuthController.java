package com.nettdet.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.RefreshToken;
import com.nettdet.backend.service.RefreshTokenService;
import com.nettdet.backend.util.JwtUtil;


@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private RefreshTokenService service;
    private JwtUtil jwtUtil;

    public AuthController(RefreshTokenService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> grantRefreshToken(@CookieValue("refresh_token") String cookie) {
        logger.info("Refresh token request received");

        RefreshToken refToken = service.getStoredRefreshToken(cookie);
        Account account = refToken.getAccount();
        logger.debug("Refresh token resolved for account: {}", account.getEmail());

        ResponseCookie refCookie = service.rotateRefreshToken(refToken);
        String jwtToken = jwtUtil.generateToken(account);
        logger.info("Issued new access and refresh token for account: {}", account.getEmail());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refCookie.toString()).body(jwtToken);
    }

}
