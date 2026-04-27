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

    // ========== GET /pupil/chapters/{chapterId}/notebook ==========

    @DisplayName("Allows authenticated pupils to fetch their chapter notebook with tips and notes")
    @Test
    void getPupilNotebook_whenAuthenticatedPupil_returnsNotebookJson() throws Exception {
        // Arrange: Create pupil principal and mock service response
        JwtUserPrincipal principal = pupilPrincipal();
        when(notebookService.getPupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("pupil@example.com") && p.role().equals("PUPIL"))))
                .thenReturn(new ChapterNotebookResponse(
                        2L,
                        "Fotografen",
                        List.of("Check the source", "Verify the URL"),
                        "My personal notes here"));

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify response structure and values
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2))
                .andExpect(jsonPath("$.tips[0]").value("Check the source"))
                .andExpect(jsonPath("$.tips[1]").value("Verify the URL"))
                .andExpect(jsonPath("$.ownNotes").value("My personal notes here"));

        // Assert: Verify service called with correct chapterId and principal
        verify(notebookService).getPupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("pupil@example.com") && p.role().equals("PUPIL")));
    }

    @DisplayName("Allows fetching notebook with empty tips and notes")
    @Test
    void getPupilNotebook_whenEmpty_returnsEmptyNotebookJson() throws Exception {
        // Arrange: Create pupil principal with empty notebook
        JwtUserPrincipal principal = pupilPrincipal();
        when(notebookService.getPupilNotebook(
                argThat(chapterId -> chapterId == 1L),
                argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(1L, "Fotografen", Collections.emptyList(), ""));

        // Act: Perform GET request
        mockMvc.perform(get("/pupil/chapters/1/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify empty arrays and strings
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(1))
                .andExpect(jsonPath("$.tips").isArray())
                .andExpect(jsonPath("$.tips").isEmpty())
                .andExpect(jsonPath("$.ownNotes").value(""));
    }

    @DisplayName("Uses JWT principal for notebook fetch, ignoring any query params")
    @Test
    void getPupilNotebook_withSpoofedQueryParams_usesJwtPrincipal() throws Exception {
        // Arrange: Create pupil principal
        JwtUserPrincipal principal = pupilPrincipal();
        when(notebookService.getPupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(2L, "Fotografen", List.of("Tip"), "Notes"));

        // Act: Perform request with malicious query params
        mockMvc.perform(get("/pupil/chapters/2/notebook?email=other@example.com&chapterId=99")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify response uses JWT identity
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2));

        // Assert: Verify service called with JWT email and path chapterId, not query params
        verify(notebookService).getPupilNotebook(
                argThat(chapterId -> chapterId == 2L && chapterId != 99L),
                argThat(p -> p.email().equals("pupil@example.com") &&
                        !p.email().equals("other@example.com")));
    }

    // ========== PUT /pupil/chapters/{chapterId}/notebook ==========

    @DisplayName("Allows authenticated pupils to update their notebook notes")
    @Test
    void updatePupilNotebook_whenAuthenticatedPupil_returnsUpdatedNotebookJson() throws Exception {
        // Arrange: Create pupil principal and request
        JwtUserPrincipal principal = pupilPrincipal();
        UpdateChapterNotebookRequest request = new UpdateChapterNotebookRequest("Updated my notes");
        when(notebookService.updatePupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(req -> req.ownNotes().equals("Updated my notes")),
                argThat(p -> p.email().equals("pupil@example.com") && p.role().equals("PUPIL"))))
                .thenReturn(new ChapterNotebookResponse(
                        2L,
                        "Fotografen",
                        List.of("Read-only tip"),
                        "Updated my notes"));

        // Act: Perform PUT request
        mockMvc.perform(put("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify response contains updated notes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2))
                .andExpect(jsonPath("$.chapterName").value("Fotografen"))
                .andExpect(jsonPath("$.tips[0]").value("Read-only tip"))
                .andExpect(jsonPath("$.ownNotes").value("Updated my notes"));

        // Assert: Verify service called with correct parameters
        verify(notebookService).updatePupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(req -> req.ownNotes().equals("Updated my notes")),
                argThat(p -> p.email().equals("pupil@example.com") && p.role().equals("PUPIL")));
    }

    @DisplayName("Updates notebook with empty notes")
    @Test
    void updatePupilNotebook_withEmptyNotes_returnsUpdatedNotebook() throws Exception {
        // Arrange: Create pupil principal and empty notes request
        JwtUserPrincipal principal = pupilPrincipal();
        UpdateChapterNotebookRequest request = new UpdateChapterNotebookRequest("");
        when(notebookService.updatePupilNotebook(
                argThat(chapterId -> chapterId == 1L),
                argThat(req -> req.ownNotes().equals("")),
                argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(1L, "Fotografen", List.of("Tip"), ""));

        // Act: Perform PUT request
        mockMvc.perform(put("/pupil/chapters/1/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify empty notes accepted
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownNotes").value(""));

        // Assert: Verify service called with empty notes
        verify(notebookService).updatePupilNotebook(
                argThat(chapterId -> chapterId == 1L),
                argThat(req -> req.ownNotes().equals("")),
                argThat(p -> p.email().equals("pupil@example.com")));
    }

    @DisplayName("Uses JWT principal for notebook update, ignoring spoofed identity params")
    @Test
    void updatePupilNotebook_ignoresSpoofedIdentityParams() throws Exception {
        // Arrange: Create pupil principal and request
        JwtUserPrincipal principal = pupilPrincipal();
        UpdateChapterNotebookRequest request = new UpdateChapterNotebookRequest("My notes");
        when(notebookService.updatePupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(req -> req.ownNotes().equals("My notes")),
                argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(2L, "Fotografen", List.of(), "My notes"));

        // Act: Perform request with malicious query params
        mockMvc.perform(put("/pupil/chapters/2/notebook?email=attacker@example.com")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify response uses JWT identity
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownNotes").value("My notes"));

        // Assert: Verify service called with JWT email, not query param
        verify(notebookService).updatePupilNotebook(
                argThat(chapterId -> chapterId == 2L),
                argThat(req -> req.ownNotes().equals("My notes")),
                argThat(p -> p.email().equals("pupil@example.com") &&
                        !p.email().equals("attacker@example.com")));
    }

    // ========== GET /teacher/classrooms/{classroomCode}/pupils/{pupilEmail}/chapters/{chapterId}/notebook ==========

    @DisplayName("Allows authenticated teachers to read pupil notebooks in their classroom")
    @Test
    void getTeacherNotebook_whenAuthenticatedTeacher_returnsNotebookJson() throws Exception {
        // Arrange: Create teacher principal and mock service
        JwtUserPrincipal principal = teacherPrincipal();
        when(notebookService.getTeacherNotebook(
                argThat(code -> code.equals("ABCABC")),
                argThat(email -> email.equals("pupil@example.com")),
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("teacher@example.com") && p.role().equals("TEACHER"))))
                .thenReturn(new ChapterNotebookResponse(
                        2L,
                        "Fotografen",
                        List.of("Teacher-visible tip"),
                        "Pupil's personal note"));

        // Act: Perform GET request
        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/pupil@example.com/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2))
                .andExpect(jsonPath("$.tips[0]").value("Teacher-visible tip"))
                .andExpect(jsonPath("$.ownNotes").value("Pupil's personal note"));

        // Assert: Verify service called with all correct parameters
        verify(notebookService).getTeacherNotebook(
                argThat(code -> code.equals("ABCABC")),
                argThat(email -> email.equals("pupil@example.com")),
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("teacher@example.com") && p.role().equals("TEACHER")));
    }

    @DisplayName("Teacher notebook endpoint uses path variables, not query params")
    @Test
    void getTeacherNotebook_ignoresQueryParamsAndUsesPathVariables() throws Exception {
        // Arrange: Create teacher principal
        JwtUserPrincipal principal = teacherPrincipal();
        when(notebookService.getTeacherNotebook(
                argThat(code -> code.equals("ABCABC")),
                argThat(email -> email.equals("pupil@example.com")),
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("teacher@example.com"))))
                .thenReturn(new ChapterNotebookResponse(2L, "Fotografen", List.of(), ""));

        // Act: Perform request with conflicting query params
        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/pupil@example.com/chapters/2/notebook" +
                        "?classroomCode=OTHER&pupilEmail=other@example.com&chapterId=99")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify path variables used
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2));

        // Assert: Verify service called with path variables, not query params
        verify(notebookService).getTeacherNotebook(
                argThat(code -> code.equals("ABCABC") && !code.equals("OTHER")),
                argThat(email -> email.equals("pupil@example.com") && !email.equals("other@example.com")),
                argThat(chapterId -> chapterId == 2L && chapterId != 99L),
                argThat(p -> p.email().equals("teacher@example.com")));
    }

    @DisplayName("Uses JWT teacher identity, ignoring spoofed query params")
    @Test
    void getTeacherNotebook_withSpoofedTeacherEmail_usesJwtPrincipal() throws Exception {
        // Arrange: Create teacher principal
        JwtUserPrincipal principal = teacherPrincipal();
        when(notebookService.getTeacherNotebook(
                argThat(code -> code.equals("ABCABC")),
                argThat(email -> email.equals("pupil@example.com")),
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("teacher@example.com"))))
                .thenReturn(new ChapterNotebookResponse(2L, "Fotografen", List.of(), ""));

        // Act: Perform request with malicious teacherEmail query param
        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/pupil@example.com/chapters/2/notebook" +
                        "?teacherEmail=other_teacher@example.com")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify JWT identity used
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(2));

        // Assert: Verify service called with JWT teacher email, not query param
        verify(notebookService).getTeacherNotebook(
                argThat(code -> code.equals("ABCABC")),
                argThat(email -> email.equals("pupil@example.com")),
                argThat(chapterId -> chapterId == 2L),
                argThat(p -> p.email().equals("teacher@example.com") &&
                        !p.email().equals("other_teacher@example.com")));
    }

    // ========== Cross-cutting authorization tests ==========

    @DisplayName("Denies teachers from using pupil notebook endpoints")
    @Test
    void getPupilNotebook_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        // Arrange: Create teacher principal
        JwtUserPrincipal principal = teacherPrincipal();

        // Act: Perform GET request to pupil endpoint with teacher auth
        mockMvc.perform(get("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service never called
        verifyNoInteractions(notebookService);
    }

    @DisplayName("Denies teachers from updating pupil notebooks")
    @Test
    void updatePupilNotebook_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        // Arrange: Create teacher principal and request
        JwtUserPrincipal principal = teacherPrincipal();
        UpdateChapterNotebookRequest request = new UpdateChapterNotebookRequest("Attempted update");

        // Act: Perform PUT request to pupil endpoint with teacher auth
        mockMvc.perform(put("/pupil/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service never called
        verifyNoInteractions(notebookService);
    }

    @DisplayName("Denies pupils from using teacher notebook endpoint")
    @Test
    void getTeacherNotebook_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        // Arrange: Create pupil principal
        JwtUserPrincipal principal = pupilPrincipal();

        // Act: Perform GET request to teacher endpoint with pupil auth
        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/other@example.com/chapters/2/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify 403 Forbidden
                .andExpect(status().isForbidden());

        // Assert: Verify service never called
        verifyNoInteractions(notebookService);
    }

    @DisplayName("Denies all notebook endpoints to unauthenticated users")
    @Test
    void allEndpoints_whenUnauthenticated_returnsUnauthorized() throws Exception {
        // Act & Assert: Test each endpoint without authentication
        mockMvc.perform(get("/pupil/chapters/2/notebook"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/pupil/chapters/2/notebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/teacher/classrooms/ABCABC/pupils/pupil@example.com/chapters/2/notebook"))
                .andExpect(status().isUnauthorized());

        // Assert: Verify service never called for any endpoint
        verifyNoInteractions(notebookService);
    }

    // ========== Edge cases ==========

    @DisplayName("Handles large chapter IDs correctly")
    @Test
    void getPupilNotebook_withLargeChapterId_handlesCorrectly() throws Exception {
        // Arrange: Create pupil principal with large chapterId
        JwtUserPrincipal principal = pupilPrincipal();
        when(notebookService.getPupilNotebook(
                argThat(chapterId -> chapterId == 999999L),
                argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(999999L, "Fotografen", List.of("Tip"), "Notes"));

        // Act: Perform GET request with large chapterId
        mockMvc.perform(get("/pupil/chapters/999999/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                // Assert: Verify large ID handled correctly
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(999999));

        // Assert: Verify service called with correct large chapterId
        verify(notebookService).getPupilNotebook(
                argThat(chapterId -> chapterId == 999999L),
                argThat(p -> p.email().equals("pupil@example.com")));
    }

    @DisplayName("Handles notebook with special characters in notes")
    @Test
    void updatePupilNotebook_withSpecialCharacters_handlesCorrectly() throws Exception {
        // Arrange: Create pupil principal with special characters in notes
        JwtUserPrincipal principal = pupilPrincipal();
        String specialNotes = "Notes with special chars: æøå émojis 🎉 <script>alert('xss')</script>";
        UpdateChapterNotebookRequest request = new UpdateChapterNotebookRequest(specialNotes);
        when(notebookService.updatePupilNotebook(
                argThat(chapterId -> chapterId == 1L),
                argThat(req -> req.ownNotes().equals(specialNotes)),
                argThat(p -> p.email().equals("pupil@example.com"))))
                .thenReturn(new ChapterNotebookResponse(1L, "Fotografen", List.of(), specialNotes));

        // Act: Perform PUT request with special characters
        mockMvc.perform(put("/pupil/chapters/1/notebook")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: Verify special characters preserved
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownNotes").value(specialNotes));

        // Assert: Verify service called with special characters
        verify(notebookService).updatePupilNotebook(
                argThat(chapterId -> chapterId == 1L),
                argThat(req -> req.ownNotes().equals(specialNotes)),
                argThat(p -> p.email().equals("pupil@example.com")));
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
