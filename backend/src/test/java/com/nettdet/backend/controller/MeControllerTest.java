package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.AuthMeResponse;
import com.nettdet.backend.dto.UpdateMeRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.AccountService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = MeController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(MeControllerTest.TestSecurityConfig.class)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @DisplayName("Allows any authenticated user to fetch their profile via GET /me")
    @Test
    void getMe_whenAuthenticatedUser_returnsProfileJson() throws Exception {
        // Arrange: Create a pupil principal and mock the service response
        JwtUserPrincipal principal = new JwtUserPrincipal("Test User", "user@example.com", "PUPIL");
        when(accountService.getAuthenticatedProfile("user@example.com"))
                .thenReturn(new AuthMeResponse(
                        "user@example.com",
                        "Test User",
                        AccountRole.PUPIL,
                        "detektiv1",
                        "ABCABC",
                        42L,
                        true));

        // Act: Perform GET /me with authentication
        mockMvc.perform(get("/me").with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify response structure and values
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.legalName").value("Test User"))
                .andExpect(jsonPath("$.role").value("PUPIL"))
                .andExpect(jsonPath("$.username").value("detektiv1"))
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.avatarFileObjectId").value(42));

        // Assert: Verify service was called with correct email
        verify(accountService).getAuthenticatedProfile(argThat(email -> email.equals("user@example.com")));
    }

    @DisplayName("Allows authenticated teachers to fetch their profile via GET /me")
    @Test
    void getMe_whenAuthenticatedTeacher_returnsProfileJson() throws Exception {
        // Arrange: Create a teacher principal and mock the service response
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
        when(accountService.getAuthenticatedProfile("teacher@example.com"))
                .thenReturn(new AuthMeResponse(
                        "teacher@example.com",
                        "Teacher Name",
                        AccountRole.TEACHER,
                        null,
                        null,
                        null,
                        false));

        // Act: Perform GET /me with teacher authentication
        mockMvc.perform(get("/me").with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify response structure with null optional fields
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.legalName").value("Teacher Name"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.classroomCode").doesNotExist())
                .andExpect(jsonPath("$.avatarFileObjectId").doesNotExist());

        // Assert: Verify service was called with correct email
        verify(accountService).getAuthenticatedProfile(argThat(email -> email.equals("teacher@example.com")));
    }

    @DisplayName("Denies unauthenticated requests to GET /me")
    @Test
    void getMe_whenUnauthenticated_returnsUnauthorized() throws Exception {
        // Act: Perform GET /me without authentication
        mockMvc.perform(get("/me"))
                // Assert: Verify 401 Unauthorized
                .andExpect(status().isUnauthorized());

        // Assert: Verify service was never called
        verifyNoInteractions(accountService);
    }

    @DisplayName("Allows authenticated pupils to update their profile via PUT /me")
    @Test
    void updateMe_whenAuthenticatedPupil_returnsUpdatedProfileJson() throws Exception {
        // Arrange: Create pupil principal and mock the service response
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        UpdateMeRequest request = new UpdateMeRequest("newUsername");
        when(accountService.updateAuthenticatedProfile(
                argThat(p -> p.email().equals("pupil@example.com") && p.role().equals("PUPIL")),
                argThat(req -> req.username().equals("newUsername"))))
                .thenReturn(new AuthMeResponse(
                        "pupil@example.com",
                        "Pupil Name",
                        AccountRole.PUPIL,
                        "newUsername",
                        "ABCABC",
                        42L,
                        true));

        // Act: Perform PUT /me with pupil authentication
        mockMvc.perform(put("/me")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify updated response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pupil@example.com"))
                .andExpect(jsonPath("$.username").value("newUsername"));

        // Assert: Verify service was called with correct principal and request
        verify(accountService).updateAuthenticatedProfile(
                argThat(p -> p.email().equals("pupil@example.com")),
                argThat(req -> req.username().equals("newUsername")));
    }

    @DisplayName("Denies teachers from updating profile via PUT /me")
    @Test
    void updateMe_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        // Arrange: Create teacher principal and request
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
        UpdateMeRequest request = new UpdateMeRequest("newUsername");

        // Act: Perform PUT /me with teacher authentication
        mockMvc.perform(put("/me")
                        .with(authentication(authenticationFor(principal, "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service was never called
        verifyNoInteractions(accountService);
    }

    @DisplayName("Denies unauthenticated requests to PUT /me")
    @Test
    void updateMe_whenUnauthenticated_returnsUnauthorized() throws Exception {
        // Arrange: Create request without authentication
        UpdateMeRequest request = new UpdateMeRequest("newUsername");

        // Act: Perform PUT /me without authentication
        mockMvc.perform(put("/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify 401 Unauthorized
                .andExpect(status().isUnauthorized());

        // Assert: Verify service was never called
        verifyNoInteractions(accountService);
    }

    @DisplayName("Uses JWT principal identity and ignores any request body identity fields")
    @Test
    void updateMe_ignoresRequestIdentityAndUsesJwtPrincipal() throws Exception {
        // Arrange: Create pupil principal and mock service to return success
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        UpdateMeRequest request = new UpdateMeRequest("detektiv1");
        when(accountService.updateAuthenticatedProfile(
                argThat(p -> p.email().equals("pupil@example.com")),
                argThat(req -> req.username().equals("detektiv1"))))
                .thenReturn(new AuthMeResponse(
                        "pupil@example.com",
                        "Pupil Name",
                        AccountRole.PUPIL,
                        "detektiv1",
                        "ABCABC",
                        42L,
                        true));

        // Act: Perform PUT /me with different email in query param (should be ignored)
        mockMvc.perform(put("/me?email=other@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify response uses JWT identity, not query param
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pupil@example.com"));

        // Assert: Verify service was called with JWT principal email, not query param
        verify(accountService).updateAuthenticatedProfile(
                argThat(p -> p.email().equals("pupil@example.com") && !p.email().equals("other@example.com")),
                argThat(req -> req.username().equals("detektiv1")));
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
