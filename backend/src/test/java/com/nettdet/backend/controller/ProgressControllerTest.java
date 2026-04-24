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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;

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

    @DisplayName("Allows authenticated pupils to fetch progress JSON")
    @Test
    void getProgress_whenAuthenticatedPupil_returnsProgressJson() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(progressService.getPupilProgress(principal)).thenReturn(List.of(
                new PupilChapterProgressResponse(1L, 3, true),
                new PupilChapterProgressResponse(2L, 0, false)));

        mockMvc.perform(get("/pupil/progress").with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chapterId").value(1))
                .andExpect(jsonPath("$[0].highscore").value(3))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[1].chapterId").value(2))
                .andExpect(jsonPath("$[1].highscore").value(0))
                .andExpect(jsonPath("$[1].unlocked").value(false));

        verify(progressService).getPupilProgress(argThat(argument ->
                argument.email().equals("pupil@example.com") && argument.role().equals("PUPIL")));
    }

    @DisplayName("Denies authenticated non-pupils from fetching pupil progress")
    @Test
    void getProgress_whenAuthenticatedNonPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        mockMvc.perform(get("/pupil/progress").with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(progressService);
    }

    @DisplayName("Returns forbidden for unauthenticated progress requests under current security configuration")
    @Test
    void getProgress_whenUnauthenticated_returnsForbiddenForCurrentSecurityConfiguration() throws Exception {
        mockMvc.perform(get("/pupil/progress"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(progressService);
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
