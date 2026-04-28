package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nettdet.backend.dto.PupilChapterProgressResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ProgressService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = ProgressController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(ProgressControllerTest.TestSecurityConfig.class)
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    @DisplayName("Allows authenticated pupils to fetch progress JSON with multiple chapters")
    @Test
    void getProgress_whenAuthenticatedPupil_returnsProgressJson() throws Exception {
        // Arrange: Create pupil principal and mock service with multiple chapters
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(progressService.getPupilProgress(argThat(p ->
                p.email().equals("pupil@example.com") &&
                p.role().equals("PUPIL") &&
                p.legalName().equals("Pupil Name"))))
                .thenReturn(List.of(
                        new PupilChapterProgressResponse(1L, 3, true),
                        new PupilChapterProgressResponse(2L, 0, false)));

        // Act: Perform GET /pupil/progress with pupil authentication
        mockMvc.perform(get("/pupil/progress").with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify response structure and values for all chapters
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chapterId").value(1))
                .andExpect(jsonPath("$[0].highscore").value(3))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[1].chapterId").value(2))
                .andExpect(jsonPath("$[1].highscore").value(0))
                .andExpect(jsonPath("$[1].unlocked").value(false));

        // Assert: Verify service was called with correct principal details
        verify(progressService).getPupilProgress(argThat(argument ->
                argument.email().equals("pupil@example.com") &&
                argument.role().equals("PUPIL")));
    }

    @DisplayName("Allows authenticated pupils to fetch empty progress list")
    @Test
    void getProgress_whenNoProgressExists_returnsEmptyList() throws Exception {
        // Arrange: Create pupil principal and mock service with empty list
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(progressService.getPupilProgress(argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(Collections.emptyList());

        // Act: Perform GET /pupil/progress
        mockMvc.perform(get("/pupil/progress").with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify empty array response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // Assert: Verify service was called
        verify(progressService).getPupilProgress(argThat(argument ->
                argument.email().equals("pupil@example.com")));
    }

    @DisplayName("Denies authenticated teachers from fetching pupil progress")
    @Test
    void getProgress_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        // Arrange: Create teacher principal
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        // Act: Perform GET /pupil/progress with teacher authentication
        mockMvc.perform(get("/pupil/progress").with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service was never called
        verifyNoInteractions(progressService);
    }

    @DisplayName("Denies authenticated users without role from fetching pupil progress")
    @Test
    void getProgress_whenAuthenticatedWithoutRole_returnsForbidden() throws Exception {
        // Arrange: Create principal without PUPIL role
        JwtUserPrincipal principal = new JwtUserPrincipal("Unknown", "unknown@example.com", "UNKNOWN");

        // Act: Perform GET /pupil/progress
        mockMvc.perform(get("/pupil/progress").with(authentication(authenticationFor(principal, "UNKNOWN"))))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service was never called
        verifyNoInteractions(progressService);
    }

    @DisplayName("Returns forbidden for unauthenticated progress requests")
    @Test
    void getProgress_whenUnauthenticated_returnsUnauthorized() throws Exception {
        // Act: Perform GET /pupil/progress without authentication
        mockMvc.perform(get("/pupil/progress"))
                // Assert: Verify 401/403 response
                .andExpect(status().isUnauthorized());

        // Assert: Verify service was never called
        verifyNoInteractions(progressService);
    }

    @DisplayName("Passes correct principal identity to service even with query params")
    @Test
    void getProgress_withQueryParams_usesJwtPrincipalNotQueryParams() throws Exception {
        // Arrange: Create pupil principal
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(progressService.getPupilProgress(argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(List.of(new PupilChapterProgressResponse(1L, 3, true)));

        // Act: Perform request with malicious query param attempting to access other user's data
        mockMvc.perform(get("/pupil/progress?email=other@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify response uses JWT identity
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chapterId").value(1));

        // Assert: Verify service was called with JWT principal email, not query param
        verify(progressService).getPupilProgress(argThat(argument ->
                argument.email().equals("pupil@example.com") &&
                !argument.email().equals("other@example.com")));
    }

    private Authentication authenticationFor(JwtUserPrincipal principal, String authority) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority)));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
