package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.nettdet.backend.dto.ClassroomLeaderboardEntryResponse;
import com.nettdet.backend.dto.ClassroomPupilLeaderboardEntryResponse;
import com.nettdet.backend.dto.ClassroomPupilLeaderboardResponse;
import com.nettdet.backend.dto.TeacherClassroomOverviewResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.LeaderboardService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = LeaderboardController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(LeaderboardControllerTest.TestSecurityConfig.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @DisplayName("Allows authenticated users to fetch global classroom leaderboard")
    @Test
    void getGlobalClassroomLeaderboard_whenAuthenticated_returnsLeaderboardJson() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(leaderboardService.getGlobalClassroomLeaderboard()).thenReturn(List.of(
                new ClassroomLeaderboardEntryResponse("ABCABC", "8A", 240L),
                new ClassroomLeaderboardEntryResponse("DEFDEF", "8B", 120L)));

        mockMvc.perform(get("/leaderboards/classrooms")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$[0].classroomName").value("8A"))
                .andExpect(jsonPath("$[0].totalXp").value(240))
                .andExpect(jsonPath("$[1].classroomCode").value("DEFDEF"));

        verify(leaderboardService).getGlobalClassroomLeaderboard();
    }

    @DisplayName("Rejects unauthenticated global classroom leaderboard requests under current security configuration")
    @Test
    void getGlobalClassroomLeaderboard_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/leaderboards/classrooms"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(leaderboardService);
    }

    @DisplayName("Allows authenticated teachers to fetch their classroom overview")
    @Test
    void getTeacherClassrooms_whenAuthenticatedTeacher_returnsTeacherOverviewJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(leaderboardService.getTeacherClassrooms(principal)).thenReturn(List.of(
                new TeacherClassroomOverviewResponse("ABCABC", "8A", 240L, 1),
                new TeacherClassroomOverviewResponse("DEFDEF", "8B", 120L, 3)));

        mockMvc.perform(get("/teacher/classrooms")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$[0].classroomName").value("8A"))
                .andExpect(jsonPath("$[0].totalScore").value(240))
                .andExpect(jsonPath("$[0].globalRank").value(1))
                .andExpect(jsonPath("$[1].globalRank").value(3));

        verify(leaderboardService).getTeacherClassrooms(argThat(argument ->
                argument.email().equals("teacher@example.com") && argument.role().equals("TEACHER")));
    }

    @DisplayName("Denies pupils from fetching teacher classroom overview")
    @Test
    void getTeacherClassrooms_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/teacher/classrooms")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(leaderboardService);
    }

    @DisplayName("Allows authenticated teachers to fetch a classroom pupil leaderboard")
    @Test
    void getClassroomLeaderboard_whenAuthenticatedTeacher_returnsClassroomLeaderboardJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(leaderboardService.getClassroomLeaderboard("ABCABC", principal))
                .thenReturn(new ClassroomPupilLeaderboardResponse(
                        "ABCABC",
                        "8A",
                        "Mandagstime for 8A",
                        List.of(
                                new ClassroomPupilLeaderboardEntryResponse("detektiv1", 240L),
                                new ClassroomPupilLeaderboardEntryResponse("detektiv2", 120L))));

        mockMvc.perform(get("/classrooms/ABCABC/leaderboard")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.classroomName").value("8A"))
                .andExpect(jsonPath("$.classroomDescription").value("Mandagstime for 8A"))
                .andExpect(jsonPath("$.entries[0].username").value("detektiv1"))
                .andExpect(jsonPath("$.entries[0].totalXp").value(240))
                .andExpect(jsonPath("$.entries[1].username").value("detektiv2"));

        verify(leaderboardService).getClassroomLeaderboard("ABCABC", principal);
    }

    @DisplayName("Denies pupils from fetching a classroom pupil leaderboard")
    @Test
    void getClassroomLeaderboard_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/classrooms/ABCABC/leaderboard")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(leaderboardService);
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
