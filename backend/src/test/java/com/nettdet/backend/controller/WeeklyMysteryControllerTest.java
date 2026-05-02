package com.nettdet.backend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import org.springframework.mock.web.MockMultipartFile;
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

import com.nettdet.backend.dto.CreateWeeklyMysteryResponse;
import com.nettdet.backend.dto.PupilWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherApprovedWeeklyMysteryResponse;
import com.nettdet.backend.dto.TeacherPendingWeeklyMysteryResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.WeeklyMysteryService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = WeeklyMysteryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(WeeklyMysteryControllerTest.TestSecurityConfig.class)
class WeeklyMysteryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklyMysteryService weeklyMysteryService;

    @DisplayName("Allows authenticated pupils to submit weekly mysteries")
    @Test
    void submitMystery_whenAuthenticatedPupil_returnsCreatedMysteryJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        MockMultipartFile file = new MockMultipartFile("file", "mystery.png", "image/png", new byte[] {1});
        when(weeklyMysteryService.submitMystery(
                argThat("ABCABC"::equals),
                argThat(argument -> argument.getOriginalFilename().equals("mystery.png")),
                argThat("Mystery comment"::equals),
                argThat("Mystery alt text"::equals),
                argThat(argument -> argument.email().equals("pupil@example.com"))))
                .thenReturn(new CreateWeeklyMysteryResponse(
                        1L,
                        10L,
                        "Mystery comment",
                        "Mystery alt text",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        null,
                        false));

        mockMvc.perform(multipart("/pupil/classrooms/ABCABC/weekly-mysteries")
                        .file(file)
                        .param("comment", "Mystery comment")
                        .param("altText", "Mystery alt text")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileObjectId").value(10))
                .andExpect(jsonPath("$.comment").value("Mystery comment"))
                .andExpect(jsonPath("$.altText").value("Mystery alt text"))
                .andExpect(jsonPath("$.approvedAt").value(nullValue()))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @DisplayName("Denies teachers from pupil weekly mystery submission endpoint")
    @Test
    void submitMystery_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "mystery.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart("/pupil/classrooms/ABCABC/weekly-mysteries")
                        .file(file)
                        .param("comment", "Mystery comment")
                        .param("altText", "Mystery alt text")
                        .with(authentication(authenticationFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(weeklyMysteryService);
    }

    @DisplayName("Allows authenticated pupils to list approved weekly mysteries")
    @Test
    void getApprovedMysteries_whenAuthenticatedPupil_returnsApprovedMysteries() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(weeklyMysteryService.getApprovedMysteries("ABCABC", principal)).thenReturn(List.of(
                new PupilWeeklyMysteryResponse(
                        2L,
                        20L,
                        "Approved mystery",
                        "Approved alt text",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        LocalDateTime.of(2026, 4, 24, 9, 30))));

        mockMvc.perform(get("/pupil/classrooms/ABCABC/weekly-mysteries")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].fileObjectId").value(20))
                .andExpect(jsonPath("$[0].comment").value("Approved mystery"))
                .andExpect(jsonPath("$[0].approvedAt").value("2026-04-24T09:30:00"));

        verify(weeklyMysteryService).getApprovedMysteries("ABCABC", principal);
    }

    @DisplayName("Allows authenticated teachers to list pending weekly mysteries")
    @Test
    void getPendingMysteries_whenAuthenticatedTeacher_returnsPendingMysteries() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(weeklyMysteryService.getPendingMysteries("ABCABC", principal)).thenReturn(List.of(
                new TeacherPendingWeeklyMysteryResponse(
                        3L,
                        30L,
                        "pupil@example.com",
                        "Pupil Name",
                        "Pending mystery",
                        "Pending alt text",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        null)));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/weekly-mysteries/pending")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].fileObjectId").value(30))
                .andExpect(jsonPath("$[0].pupilEmail").value("pupil@example.com"))
                .andExpect(jsonPath("$[0].pupilLegalName").value("Pupil Name"))
                .andExpect(jsonPath("$[0].approvedAt").value(nullValue()));
    }

    @DisplayName("Allows authenticated teachers to list approved weekly mysteries")
    @Test
    void getApprovedMysteriesForTeacher_whenAuthenticatedTeacher_returnsApprovedMysteries() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(weeklyMysteryService.getApprovedMysteriesForTeacher("ABCABC", principal)).thenReturn(List.of(
                new TeacherApprovedWeeklyMysteryResponse(
                        6L,
                        60L,
                        "Pupil Name",
                        "Approved mystery",
                        "Approved alt text",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        LocalDateTime.of(2026, 4, 24, 9, 30))));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/weekly-mysteries/approved")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6))
                .andExpect(jsonPath("$[0].fileObjectId").value(60))
                .andExpect(jsonPath("$[0].pupilLegalName").value("Pupil Name"))
                .andExpect(jsonPath("$[0].comment").value("Approved mystery"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-04-23T10:15:00"))
                .andExpect(jsonPath("$[0].approvedAt").value("2026-04-24T09:30:00"));

        verify(weeklyMysteryService).getApprovedMysteriesForTeacher("ABCABC", principal);
    }

    @DisplayName("Allows authenticated teachers to accept weekly mysteries")
    @Test
    void acceptMystery_whenAuthenticatedTeacher_returnsApprovedMystery() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(weeklyMysteryService.acceptMystery("ABCABC", 4L, null, principal))
                .thenReturn(new PupilWeeklyMysteryResponse(
                        4L,
                        40L,
                        "Approved mystery",
                        "Approved alt text",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        LocalDateTime.of(2026, 4, 24, 9, 30)));

        mockMvc.perform(put("/teacher/classrooms/ABCABC/weekly-mysteries/4/accept")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.fileObjectId").value(40))
                .andExpect(jsonPath("$.comment").value("Approved mystery"))
                .andExpect(jsonPath("$.approvedAt").value("2026-04-24T09:30:00"));

        verify(weeklyMysteryService).acceptMystery("ABCABC", 4L, null, principal);
    }

    @DisplayName("Allows authenticated teachers to accept weekly mysteries with alt text")
    @Test
    void acceptMystery_whenAuthenticatedTeacherWithAltText_returnsApprovedMystery() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(weeklyMysteryService.acceptMystery("ABCABC", 4L, "Teacher alt text", principal))
                .thenReturn(new PupilWeeklyMysteryResponse(
                        4L,
                        40L,
                        "Approved mystery",
                        "Teacher alt text",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        LocalDateTime.of(2026, 4, 24, 9, 30)));

        mockMvc.perform(put("/teacher/classrooms/ABCABC/weekly-mysteries/4/accept")
                        .param("altText", "Teacher alt text")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.fileObjectId").value(40))
                .andExpect(jsonPath("$.comment").value("Approved mystery"))
                .andExpect(jsonPath("$.approvedAt").value("2026-04-24T09:30:00"));

        verify(weeklyMysteryService).acceptMystery("ABCABC", 4L, "Teacher alt text", principal);
    }

    @DisplayName("Allows authenticated teachers to decline weekly mysteries")
    @Test
    void declineMystery_whenAuthenticatedTeacher_returnsNoContent() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();

        mockMvc.perform(delete("/teacher/classrooms/ABCABC/weekly-mysteries/5")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isNoContent());

        verify(weeklyMysteryService).declineMystery("ABCABC", 5L, principal);
    }

    @DisplayName("Denies pupils from teacher pending weekly mystery endpoint")
    @Test
    void getPendingMysteries_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        mockMvc.perform(get("/teacher/classrooms/ABCABC/weekly-mysteries/pending")
                        .with(authentication(authenticationFor(pupilPrincipal(), "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(weeklyMysteryService);
    }

    @DisplayName("Denies pupils from teacher approved weekly mystery endpoint")
    @Test
    void getApprovedMysteriesForTeacher_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        mockMvc.perform(get("/teacher/classrooms/ABCABC/weekly-mysteries/approved")
                        .with(authentication(authenticationFor(pupilPrincipal(), "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(weeklyMysteryService);
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
