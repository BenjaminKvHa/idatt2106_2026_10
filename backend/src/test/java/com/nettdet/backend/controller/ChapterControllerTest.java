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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nettdet.backend.dto.ChapterProgressionResponse;
import com.nettdet.backend.dto.ChapterResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ChapterService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = ChapterController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(ChapterControllerTest.TestSecurityConfig.class)
class ChapterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChapterService chapterService;

    @DisplayName("Allows authenticated users to list chapters")
    @Test
    void getAllChapters_whenAuthenticated_returnsChapterJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(chapterService.getAllChapters()).thenReturn(List.of(
                new ChapterResponse(1L, "Nyhetskvartalet"),
                new ChapterResponse(2L, "Fotografen")));

        mockMvc.perform(get("/chapters")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].chapterName").value("Nyhetskvartalet"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].chapterName").value("Fotografen"));

        verify(chapterService).getAllChapters();
    }

    @DisplayName("Rejects unauthenticated chapter listing under current security configuration")
    @Test
    void getAllChapters_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/chapters"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chapterService);
    }

    @DisplayName("Allows authenticated pupils to fetch their chapter progression")
    @Test
    void getProgression_whenAuthenticatedPupil_returnsProgressionJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(chapterService.getProgression("pupil@example.com", 1L))
                .thenReturn(new ChapterProgressionResponse(2, 1, 3, 4, 5));

        mockMvc.perform(get("/chapters/1/progression")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRunAttempts").value(2))
                .andExpect(jsonPath("$.currentRunCorrect").value(1))
                .andExpect(jsonPath("$.successfulRuns").value(3))
                .andExpect(jsonPath("$.highscore").value(4))
                .andExpect(jsonPath("$.unsuccessfulStreak").value(5));

        verify(chapterService).getProgression(
                argThat("pupil@example.com"::equals),
                argThat(chapterId -> chapterId == 1L));
    }

    @DisplayName("Denies teachers from pupil chapter progression endpoint")
    @Test
    void getProgression_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        mockMvc.perform(get("/chapters/1/progression")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chapterService);
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
