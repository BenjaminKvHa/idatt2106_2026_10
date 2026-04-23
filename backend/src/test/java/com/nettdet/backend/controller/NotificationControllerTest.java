package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nettdet.backend.domain.enums.NotificationStatus;
import com.nettdet.backend.dto.TeacherNotificationResponse;
import com.nettdet.backend.dto.UnreadNotificationsResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.NotificationService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(NotificationControllerTest.TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @DisplayName("Allows authenticated teachers to fetch classroom notifications")
    @Test
    void getTeacherNotifications_whenAuthenticatedTeacher_returnsNotificationJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(notificationService.getTeacherNotifications("ABCABC", principal)).thenReturn(List.of(
                new TeacherNotificationResponse(
                        1L,
                        "ABCABC",
                        "Pupil Name",
                        "Pupil requested help",
                        LocalDateTime.of(2026, 4, 23, 10, 15),
                        NotificationStatus.UNREAD)));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/notifications")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$[0].pupilLegalName").value("Pupil Name"))
                .andExpect(jsonPath("$[0].text").value("Pupil requested help"))
                .andExpect(jsonPath("$[0].statusNotifications").value("UNREAD"));

        verify(notificationService).getTeacherNotifications("ABCABC", principal);
    }

    @DisplayName("Allows authenticated teachers to check unread classroom notifications")
    @Test
    void hasUnreadNotifications_whenAuthenticatedTeacher_returnsUnreadStateJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(notificationService.hasUnreadNotifications("ABCABC", principal))
                .thenReturn(new UnreadNotificationsResponse("ABCABC", true));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/notifications/unread")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.hasUnreadNotifications").value(true));

        verify(notificationService).hasUnreadNotifications("ABCABC", principal);
    }

    @DisplayName("Denies authenticated pupils from fetching teacher notifications")
    @Test
    void getTeacherNotifications_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/teacher/classrooms/ABCABC/notifications")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @DisplayName("Delegates teacher identity from JWT principal instead of request input")
    @Test
    void hasUnreadNotifications_whenAuthenticatedTeacher_delegatesJwtPrincipal() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(notificationService.hasUnreadNotifications("ABCABC", principal))
                .thenReturn(new UnreadNotificationsResponse("ABCABC", false));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/notifications/unread?teacherEmail=other@example.com")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnreadNotifications").value(false));

        verify(notificationService).hasUnreadNotifications(
                argThat("ABCABC"::equals),
                argThat(argument -> argument.email().equals("teacher@example.com") && argument.role().equals("TEACHER")));
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
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
