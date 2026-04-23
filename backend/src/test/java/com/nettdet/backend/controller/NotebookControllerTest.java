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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.dto.ChapterNotebookResponse;
import com.nettdet.backend.dto.UpdateChapterNotebookRequest;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.NotebookService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = NotebookController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(NotebookControllerTest.TestSecurityConfig.class)
class NotebookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotebookService notebookService;

    @DisplayName("Allows authenticated pupils to fetch their chapter notebook")
    @Test
    void getPupilNotebook_whenAuthenticatedPupil_returnsNotebookJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(notebookService.getPupilNotebook(2L, principal)).thenReturn(new ChapterNotebookResponse(
                2L,
                List.of("Check source"),
                "My notes"));

        mockMvc.perform(get("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2))
                .andExpect(jsonPath("$.tips[0]").value("Check source"))
                .andExpect(jsonPath("$.ownNotes").value("My notes"));

        verify(notebookService).getPupilNotebook(2L, principal);
    }

    @DisplayName("Allows authenticated pupils to update only own notebook notes")
    @Test
    void updatePupilNotebook_whenAuthenticatedPupil_returnsUpdatedNotebookJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(notebookService.updatePupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(request -> request.ownNotes().equals("Updated notes")),
                argThat(argument -> argument.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(2L, List.of("Read-only tip"), "Updated notes"));

        mockMvc.perform(put("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateChapterNotebookRequest("Updated notes"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2))
                .andExpect(jsonPath("$.tips[0]").value("Read-only tip"))
                .andExpect(jsonPath("$.ownNotes").value("Updated notes"));

        verify(notebookService).updatePupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(request -> request.ownNotes().equals("Updated notes")),
                argThat(argument -> argument.email().equals("pupil@example.com") && argument.role().equals("PUPIL")));
    }

    @DisplayName("Denies teachers from using pupil notebook endpoints")
    @Test
    void getPupilNotebook_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();

        mockMvc.perform(get("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notebookService);
    }

    @DisplayName("Allows authenticated teachers to read pupil notebooks in a classroom")
    @Test
    void getTeacherNotebook_whenAuthenticatedTeacher_returnsNotebookJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(notebookService.getTeacherNotebook("ABCABC", "pupil@example.com", 2L, principal))
                .thenReturn(new ChapterNotebookResponse(2L, List.of("Teacher-visible tip"), "Pupil note"));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/pupil@example.com/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2))
                .andExpect(jsonPath("$.tips[0]").value("Teacher-visible tip"))
                .andExpect(jsonPath("$.ownNotes").value("Pupil note"));

        verify(notebookService).getTeacherNotebook("ABCABC", "pupil@example.com", 2L, principal);
    }

    @DisplayName("Denies pupils from using teacher notebook endpoint")
    @Test
    void getTeacherNotebook_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();

        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/other@example.com/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notebookService);
    }

    @DisplayName("Delegates teacher notebook identity from JWT principal and path variables")
    @Test
    void getTeacherNotebook_whenRequestIncludesExtraIdentityParameter_ignoresQueryIdentity() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(notebookService.getTeacherNotebook("ABCABC", "pupil@example.com", 2L, principal))
                .thenReturn(new ChapterNotebookResponse(2L, List.of(), ""));

        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/pupil@example.com/chapters/2/notebook?teacherEmail=other@example.com")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2));

        verify(notebookService).getTeacherNotebook(
                argThat("ABCABC"::equals),
                argThat("pupil@example.com"::equals),
                argThat(chapterId -> chapterId == 2L),
                argThat(argument -> argument.email().equals("teacher@example.com") && argument.role().equals("TEACHER")));
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
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
