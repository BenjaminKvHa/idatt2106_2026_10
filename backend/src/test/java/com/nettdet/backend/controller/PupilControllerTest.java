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

    @DisplayName("Allows authenticated pupils to fetch their classroom information")
    @Test
    void getPupilInformation_whenAuthenticatedPupil_returnsPupilJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilInformation("pupil@example.com", "ABCABC"))
                .thenReturn(new PupilResponse(
                        "pupil@example.com",
                        "detektiv1",
                        "Pupil Name",
                        42L,
                        "ABCABC",
                        true,
                        List.of(new PupilResponse.ChapterProgression(1L, 2, 3, 0))));

        mockMvc.perform(get("/pupil/classrooms/ABCABC")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pupil@example.com"))
                .andExpect(jsonPath("$.username").value("detektiv1"))
                .andExpect(jsonPath("$.legalName").value("Pupil Name"))
                .andExpect(jsonPath("$.avatarFileObjectId").value(42))
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(true))
                .andExpect(jsonPath("$.chapterProgressions[0].chapterId").value(1));

        verify(pupilService).getPupilInformation("pupil@example.com", "ABCABC");
    }

    @DisplayName("Allows authenticated pupils to fetch classroom progression")
    @Test
    void getPupilProgression_whenAuthenticatedPupil_returnsProgressionJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.getPupilProgressionPerChapter("pupil@example.com", "ABCABC"))
                .thenReturn(new PupilProgressionResponse(List.of(
                        new PupilResponse.ChapterProgression(1L, 2, 3, 0))));

        mockMvc.perform(get("/pupil/classrooms/ABCABC/progression")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterProgressions[0].chapterId").value(1))
                .andExpect(jsonPath("$.chapterProgressions[0].successfulRuns").value(2))
                .andExpect(jsonPath("$.chapterProgressions[0].highscore").value(3))
                .andExpect(jsonPath("$.chapterProgressions[0].unsuccessfulStreak").value(0));

        verify(pupilService).getPupilProgressionPerChapter("pupil@example.com", "ABCABC");
    }

    @DisplayName("Returns OK when authenticated pupil has confirmed classroom membership")
    @Test
    void checkMembership_whenMembershipIsConfirmed_returnsOk() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.checkPupilMembershipForClass(new PupilRequest("pupil@example.com", "ABCABC")))
                .thenReturn(true);

        mockMvc.perform(get("/pupil/classrooms/ABCABC/membership")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk());

        verify(pupilService).checkPupilMembershipForClass(new PupilRequest("pupil@example.com", "ABCABC"));
    }

    @DisplayName("Returns forbidden when authenticated pupil does not have confirmed classroom membership")
    @Test
    void checkMembership_whenMembershipIsMissingOrUnconfirmed_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.checkPupilMembershipForClass(new PupilRequest("pupil@example.com", "ABCABC")))
                .thenReturn(false);

        mockMvc.perform(get("/pupil/classrooms/ABCABC/membership")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verify(pupilService).checkPupilMembershipForClass(new PupilRequest("pupil@example.com", "ABCABC"));
    }

    @DisplayName("Creates join request using authenticated pupil identity")
    @Test
    void requestToJoinClassroom_whenAuthenticatedPupil_returnsCreatedPupilResponse() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.requestToJoinClassroom(new PupilRequest("pupil@example.com", "ABCABC")))
                .thenReturn(new PupilResponse(
                        "pupil@example.com",
                        null,
                        "Pupil Name",
                        null,
                        "ABCABC",
                        false,
                        List.of()));

        mockMvc.perform(post("/pupil/classrooms/ABCABC/join")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pupil@example.com"))
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(false));

        verify(pupilService).requestToJoinClassroom(new PupilRequest("pupil@example.com", "ABCABC"));
    }

    @DisplayName("Updates classroom membership username and avatar using authenticated pupil identity")
    @Test
    void setClassroomMembership_whenAuthenticatedPupil_returnsUpdatedMembershipJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.updateClassroomMembership(
                argThat("pupil@example.com"::equals),
                argThat("ABCABC"::equals),
                argThat("detektiv1"::equals),
                argThat("boy_hair1_costume2"::equals)))
                .thenReturn(new PupilClassroomMembershipResponse("detektiv1", 42L, false));

        mockMvc.perform(put("/pupil/classrooms/ABCABC/membership")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePupilClassroomMembershipRequest(
                                "detektiv1",
                                "boy_hair1_costume2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("detektiv1"))
                .andExpect(jsonPath("$.avatarFileObjectId").value(42))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(false));

        verify(pupilService).updateClassroomMembership(
                "pupil@example.com",
                "ABCABC",
                "detektiv1",
                "boy_hair1_costume2");
    }

    @DisplayName("Denies teachers from pupil endpoints")
    @Test
    void getPupilInformation_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        mockMvc.perform(get("/pupil/classrooms/ABCABC")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pupilService);
    }

    @DisplayName("Ignores request identity parameters and uses the JWT pupil identity")
    @Test
    void requestToJoinClassroom_whenQueryContainsDifferentEmail_usesJwtPrincipalEmail() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(pupilService.requestToJoinClassroom(new PupilRequest("pupil@example.com", "ABCABC")))
                .thenReturn(new PupilResponse(
                        "pupil@example.com",
                        null,
                        "Pupil Name",
                        null,
                        "ABCABC",
                        false,
                        List.of()));

        mockMvc.perform(post("/pupil/classrooms/ABCABC/join?email=other@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pupil@example.com"));

        verify(pupilService).requestToJoinClassroom(new PupilRequest("pupil@example.com", "ABCABC"));
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
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
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
