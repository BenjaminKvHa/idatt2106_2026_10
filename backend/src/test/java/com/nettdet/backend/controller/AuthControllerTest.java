package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.RefreshToken;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.LoginRequest;
import com.nettdet.backend.dto.RegisterRequest;
import com.nettdet.backend.service.AccountService;
import com.nettdet.backend.service.RefreshTokenService;
import com.nettdet.backend.util.JwtAuthFilter;
import com.nettdet.backend.util.JwtUtil;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @DisplayName("Login returns access token JSON and refresh-token cookie")
    @Test
    void login_whenCredentialsAreValid_returnsAccessTokenAndRefreshCookie() throws Exception {
        Account account = account("teacher@example.com", "Teacher Name", AccountRole.TEACHER);
        LoginRequest request = new LoginRequest("teacher@example.com", "Test1234");
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-token")
                .httpOnly(true)
                .path("/api/auth")
                .build();
        when(accountService.login(any(LoginRequest.class))).thenReturn(account);
        when(jwtUtil.generateToken(account)).thenReturn("access-token");
        when(refreshTokenService.createAndStoreRefreshToken(account)).thenReturn(refreshCookie);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", refreshCookie.toString()))
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(accountService).login(argThat(argument ->
                argument.email().equals("teacher@example.com") && argument.password().equals("Test1234")));
    }

    @DisplayName("Signup registers account and immediately returns access token with refresh-token cookie")
    @Test
    void register_whenRequestIsValid_returnsAccessTokenAndRefreshCookie() throws Exception {
        Account account = account("pupil@example.com", "Pupil Name", AccountRole.PUPIL);
        RegisterRequest request = new RegisterRequest(
                "pupil@example.com",
                "Test1234",
                "Pupil Name",
                AccountRole.PUPIL);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-token")
                .httpOnly(true)
                .path("/api/auth")
                .build();
        when(accountService.register(any(RegisterRequest.class))).thenReturn(account);
        when(jwtUtil.generateToken(account)).thenReturn("access-token");
        when(refreshTokenService.createAndStoreRefreshToken(account)).thenReturn(refreshCookie);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", refreshCookie.toString()))
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(accountService).register(argThat(argument ->
                argument.email().equals("pupil@example.com")
                        && argument.password().equals("Test1234")
                        && argument.legalName().equals("Pupil Name")
                        && argument.role() == AccountRole.PUPIL));
    }

    @DisplayName("Refresh rotates the refresh cookie and returns a new access token")
    @Test
    void refresh_whenCookieIsValid_returnsAccessTokenAndRotatedCookie() throws Exception {
        Account account = account("teacher@example.com", "Teacher Name", AccountRole.TEACHER);
        RefreshToken token = new RefreshToken();
        token.setAccount(account);
        ResponseCookie rotatedCookie = ResponseCookie.from("refresh_token", "rotated").httpOnly(true).path("/api/auth").build();
        when(refreshTokenService.getStoredRefreshToken("old-token")).thenReturn(token);
        when(refreshTokenService.rotateRefreshToken(token)).thenReturn(rotatedCookie);
        when(jwtUtil.generateToken(account)).thenReturn("new-access-token");

        mockMvc.perform(post("/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", rotatedCookie.toString()))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));

        verify(refreshTokenService).getStoredRefreshToken("old-token");
        verify(refreshTokenService).rotateRefreshToken(token);
    }

    @DisplayName("Logout invalidates the refresh cookie")
    @Test
    void logout_whenCookieProvided_returnsExpiredCookie() throws Exception {
        ResponseCookie expired = ResponseCookie.from("refresh_token", "").maxAge(0).path("/api/auth").build();
        when(refreshTokenService.logout("token")).thenReturn(expired);

        mockMvc.perform(post("/auth/logout").cookie(new jakarta.servlet.http.Cookie("refresh_token", "token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("refresh_token="),
                        containsString("Path=/api/auth"),
                        containsString("Max-Age=0"))));

        verify(refreshTokenService).logout("token");
    }

    private Account account(String email, String legalName, AccountRole role) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(legalName);
        account.setAccountRole(role);
        account.setPasswordData("encoded-password");
        return account;
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
}
