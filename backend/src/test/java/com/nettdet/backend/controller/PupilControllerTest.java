package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.nettdet.backend.dto.PupilClassroomMembershipResponse;
import com.nettdet.backend.dto.PupilProgressionResponse;
import com.nettdet.backend.dto.PupilRequest;
import com.nettdet.backend.dto.PupilResponse;
import com.nettdet.backend.dto.UpdatePupilClassroomMembershipRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.PupilService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = PupilController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(PupilControllerTest.TestSecurityConfig.class)
class PupilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PupilService pupilService;

    // ========== GET /pupil/classrooms/{classroomCode} ==========

    @DisplayName("Allows authenticated pupils to fetch their classroom information")
    @Test
    void getPupilInformation_whenAuthenticatedPupil_returnsPupilJson() throws Exception {
        // Arrange: Create pupil principal and mock service response
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilInformation(
                argThat(email -> email.equals("pupil@example.com")),
                argThat(code -> code.equals("ABCABC"))))
                .thenReturn(new PupilResponse(
                        "pupil@example.com",
                        "detektiv1",
                        "Pupil Name",
                        42L,
                        "ABCABC",
                        true,
                        List.of(new PupilResponse.ChapterProgression(1L, 2, 3, 0))));

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/classrooms/ABCABC")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify all response fields
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pupil@example.com"))
                .andExpect(jsonPath("$.username").value("detektiv1"))
                .andExpect(jsonPath("$.legalName").value("Pupil Name"))
                .andExpect(jsonPath("$.avatarFileObjectId").value(42))
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(true))
                .andExpect(jsonPath("$.chapterProgressions[0].chapterId").value(1))
                .andExpect(jsonPath("$.chapterProgressions[0].successfulRuns").value(2))
                .andExpect(jsonPath("$.chapterProgressions[0].highscore").value(3))
                .andExpect(jsonPath("$.chapterProgressions[0].unsuccessfulStreak").value(0));

        // Assert: Verify service called with correct parameters
        verify(pupilService).getPupilInformation("pupil@example.com", "ABCABC");
    }

    @DisplayName("Allows fetching classroom info for unconfirmed membership")
    @Test
    void getPupilInformation_whenMembershipUnconfirmed_returnsUnconfirmedJson() throws Exception {
        // Arrange: Create pupil principal with unconfirmed membership
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilInformation("pupil@example.com", "ABCABC"))
                .thenReturn(new PupilResponse(
                        "pupil@example.com",
                        null,
                        "Pupil Name",
                        null,
                        "ABCABC",
                        false,
                        Collections.emptyList()));

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/classrooms/ABCABC")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify unconfirmed state in response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(false))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.avatarFileObjectId").doesNotExist())
                .andExpect(jsonPath("$.chapterProgressions").isEmpty());
    }

    @DisplayName("Uses JWT identity when query params attempt to spoof different user")
    @Test
    void getPupilInformation_withSpoofedEmailQuery_usesJwtPrincipal() throws Exception {
        // Arrange: Create pupil principal
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilInformation("pupil@example.com", "ABCABC"))
                .thenReturn(new PupilResponse(
                        "pupil@example.com", "detektiv1", "Pupil Name",
                        42L, "ABCABC", true, Collections.emptyList()));

        // Act: Perform request with malicious email query param
        mockMvc.perform(get("/pupil/classrooms/ABCABC?email=attacker@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify JWT identity used, not query param
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pupil@example.com"));

        // Assert: Verify service called with JWT email, not query param
        verify(pupilService).getPupilInformation(
                argThat(email -> email.equals("pupil@example.com") && !email.equals("attacker@example.com")),
                argThat(code -> code.equals("ABCABC")));
    }

    @DisplayName("Denies teachers from accessing pupil classroom information")
    @Test
    void getPupilInformation_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        // Arrange: Create teacher principal
        JwtUserPrincipal principal = teacherPrincipal();

        // Act: Perform GET request with teacher authentication
        mockMvc.perform(get("/pupil/classrooms/ABCABC")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service never called
        verifyNoInteractions(pupilService);
    }

    // ========== GET /pupil/classrooms/{classroomCode}/progression ==========

    @DisplayName("Allows authenticated pupils to fetch classroom progression")
    @Test
    void getPupilProgression_whenAuthenticatedPupil_returnsProgressionJson() throws Exception {
        // Arrange: Create pupil principal and mock service
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilProgressionPerChapter(
                argThat(email -> email.equals("pupil@example.com")),
                argThat(code -> code.equals("ABCABC"))))
                .thenReturn(new PupilProgressionResponse(List.of(
                        new PupilResponse.ChapterProgression(1L, 2, 3, 0),
                        new PupilResponse.ChapterProgression(2L, 1, 2, 1))));

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/classrooms/ABCABC/progression")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify progression data
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterProgressions[0].chapterId").value(1))
                .andExpect(jsonPath("$.chapterProgressions[0].successfulRuns").value(2))
                .andExpect(jsonPath("$.chapterProgressions[0].highscore").value(3))
                .andExpect(jsonPath("$.chapterProgressions[0].unsuccessfulStreak").value(0))
                .andExpect(jsonPath("$.chapterProgressions[1].chapterId").value(2))
                .andExpect(jsonPath("$.chapterProgressions[1].successfulRuns").value(1))
                .andExpect(jsonPath("$.chapterProgressions[1].highscore").value(2))
                .andExpect(jsonPath("$.chapterProgressions[1].unsuccessfulStreak").value(1));

        // Assert: Verify service called with correct parameters
        verify(pupilService).getPupilProgressionPerChapter("pupil@example.com", "ABCABC");
    }

    @DisplayName("Returns empty progression list when no chapters available")
    @Test
    void getPupilProgression_whenNoChapters_returnsEmptyProgression() throws Exception {
        // Arrange: Create pupil principal with empty progression
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilProgressionPerChapter("pupil@example.com", "ABCABC"))
                .thenReturn(new PupilProgressionResponse(Collections.emptyList()));

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/classrooms/ABCABC/progression")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify empty progression array
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterProgressions").isArray())
                .andExpect(jsonPath("$.chapterProgressions").isEmpty());
    }

    // ========== GET /pupil/classrooms/{classroomCode}/membership ==========

    @DisplayName("Returns OK when authenticated pupil has confirmed classroom membership")
    @Test
    void checkMembership_whenMembershipIsConfirmed_returnsOk() throws Exception {
        // Arrange: Create pupil principal with confirmed membership
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.checkPupilMembershipForClass(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        req.classroomCode().equals("ABCABC"))))
                .thenReturn(true);

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/classrooms/ABCABC/membership")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify 200 OK
                .andExpect(status().isOk());

        // Assert: Verify service called with correct PupilRequest
        verify(pupilService).checkPupilMembershipForClass(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        req.classroomCode().equals("ABCABC")));
    }

    @DisplayName("Returns forbidden when authenticated pupil does not have confirmed membership")
    @Test
    void checkMembership_whenMembershipIsMissingOrUnconfirmed_returnsForbidden() throws Exception {
        // Arrange: Create pupil principal without confirmed membership
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.checkPupilMembershipForClass(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        req.classroomCode().equals("ABCABC"))))
                .thenReturn(false);

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/classrooms/ABCABC/membership")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service was called
        verify(pupilService).checkPupilMembershipForClass(
                argThat(req -> req.email().equals("pupil@example.com")));
    }

    @DisplayName("Uses JWT principal for membership check, ignoring query params")
    @Test
    void checkMembership_withSpoofedQueryParams_usesJwtPrincipal() throws Exception {
        // Arrange: Create pupil principal
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.checkPupilMembershipForClass(
                argThat(req -> req.email().equals("pupil@example.com"))))
                .thenReturn(true);

        // Act: Perform request with malicious query params
        mockMvc.perform(get("/pupil/classrooms/ABCABC/membership?email=other@example.com&classroomCode=OTHER")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify OK response (JWT identity used)
                .andExpect(status().isOk());

        // Assert: Verify service called with JWT email, not query params
        verify(pupilService).checkPupilMembershipForClass(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        req.classroomCode().equals("ABCABC") &&
                        !req.email().equals("other@example.com")));
    }

    // ========== POST /pupil/intro/complete ==========

    @DisplayName("Completes first-time intro using authenticated pupil identity")
    @Test
    void completeIntroSequence_whenAuthenticatedPupil_returnsNoContent() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();

        mockMvc.perform(post("/pupil/intro/complete")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isNoContent());

        verify(pupilService).completeIntroSequence("pupil@example.com");
    }

    @DisplayName("Denies teachers from completing pupil intro")
    @Test
    void completeIntroSequence_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();

        mockMvc.perform(post("/pupil/intro/complete")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pupilService);
    }

    // ========== POST /pupil/classrooms/{classroomCode}/join ==========

    @DisplayName("Creates join request using authenticated pupil identity")
    @Test
    void requestToJoinClassroom_whenAuthenticatedPupil_returnsCreatedPupilResponse() throws Exception {
        // Arrange: Create pupil principal and mock service
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.requestToJoinClassroom(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        req.classroomCode().equals("ABCABC"))))
                .thenReturn(new PupilResponse(
                        "pupil@example.com",
                        null,
                        "Pupil Name",
                        null,
                        "ABCABC",
                        false,
                        Collections.emptyList()));

        // Act: Perform POST request
        mockMvc.perform(post("/pupil/classrooms/ABCABC/join")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify 201 Created and response body
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pupil@example.com"))
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(false))
                .andExpect(jsonPath("$.username").doesNotExist());

        // Assert: Verify service called with correct PupilRequest
        verify(pupilService).requestToJoinClassroom(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        req.classroomCode().equals("ABCABC")));
    }

    @DisplayName("Ignores request body/query params and uses JWT pupil identity for join")
    @Test
    void requestToJoinClassroom_whenQueryContainsDifferentEmail_usesJwtPrincipalEmail() throws Exception {
        // Arrange: Create pupil principal
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.requestToJoinClassroom(
                argThat(req -> req.email().equals("pupil@example.com"))))
                .thenReturn(new PupilResponse(
                        "pupil@example.com", null, "Pupil Name",
                        null, "ABCABC", false, Collections.emptyList()));

        // Act: Perform request with malicious query param
        mockMvc.perform(post("/pupil/classrooms/ABCABC/join?email=other@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify JWT identity used in response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pupil@example.com"));

        // Assert: Verify service called with JWT email, not query param
        verify(pupilService).requestToJoinClassroom(
                argThat(req -> req.email().equals("pupil@example.com") &&
                        !req.email().equals("other@example.com")));
    }

    // ========== PUT /pupil/classrooms/{classroomCode}/membership ==========

    @DisplayName("Updates classroom membership username and avatar using authenticated pupil identity")
    @Test
    void setClassroomMembership_whenAuthenticatedPupil_returnsUpdatedMembershipJson() throws Exception {
        // Arrange: Create pupil principal and mock service
        JwtUserPrincipal principal = pupilPrincipal();
        UpdatePupilClassroomMembershipRequest request =
                new UpdatePupilClassroomMembershipRequest("detektiv1", "boy_hair1_costume2");
        when(pupilService.updateClassroomMembership(
                argThat(email -> email.equals("pupil@example.com")),
                argThat(code -> code.equals("ABCABC")),
                argThat(username -> username.equals("detektiv1")),
                argThat(avatar -> avatar.equals("boy_hair1_costume2"))))
                .thenReturn(new PupilClassroomMembershipResponse("detektiv1", 42L, false));

        // Act: Perform PUT request
        mockMvc.perform(put("/pupil/classrooms/ABCABC/membership")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("detektiv1"))
                .andExpect(jsonPath("$.avatarFileObjectId").value(42))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(false));

        // Assert: Verify service called with all correct parameters
        verify(pupilService).updateClassroomMembership(
                "pupil@example.com",
                "ABCABC",
                "detektiv1",
                "boy_hair1_costume2");
    }

    @DisplayName("Uses JWT identity for membership update, ignoring any identity in request")
    @Test
    void setClassroomMembership_ignoresSpoofedIdentityInRequestBody() throws Exception {
        // Arrange: Create pupil principal and request
        JwtUserPrincipal principal = pupilPrincipal();
        UpdatePupilClassroomMembershipRequest request =
                new UpdatePupilClassroomMembershipRequest("detektiv1", "boy_hair1_costume2");
        when(pupilService.updateClassroomMembership(
                argThat(email -> email.equals("pupil@example.com")),
                argThat(code -> code.equals("ABCABC")),
                argThat(username -> username.equals("detektiv1")),
                argThat(avatar -> avatar.equals("boy_hair1_costume2"))))
                .thenReturn(new PupilClassroomMembershipResponse("detektiv1", 42L, true));

        // Act: Perform request with malicious query param
        mockMvc.perform(put("/pupil/classrooms/ABCABC/membership?email=other@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify response uses JWT identity
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("detektiv1"));

        // Assert: Verify service called with JWT email, not query param
        verify(pupilService).updateClassroomMembership(
                argThat(email -> email.equals("pupil@example.com") && !email.equals("other@example.com")),
                argThat(code -> code.equals("ABCABC")),
                argThat(username -> username.equals("detektiv1")),
                argThat(avatar -> avatar.equals("boy_hair1_costume2")));
    }

    // ========== Cross-cutting security tests ==========

    @DisplayName("Denies all pupil endpoints to unauthenticated users")
    @Test
    void allEndpoints_whenUnauthenticated_returnsUnauthorized() throws Exception {
        // Act & Assert: Test each endpoint without authentication
        mockMvc.perform(get("/pupil/classrooms/ABCABC"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pupil/classrooms/ABCABC/progression"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pupil/classrooms/ABCABC/membership"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/pupil/intro/complete"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/pupil/classrooms/ABCABC/join"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/pupil/classrooms/ABCABC/membership")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // Assert: Verify service never called for any endpoint
        verifyNoInteractions(pupilService);
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
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
