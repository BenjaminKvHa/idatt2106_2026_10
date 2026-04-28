package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.TaskAdminResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.TaskAdminService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = TaskAdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(TaskAdminControllerTest.TestSecurityConfig.class)
class TaskAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskAdminService taskAdminService;

    private MockMultipartFile metadataPart(String json) {
        return new MockMultipartFile("metadata", "metadata", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    private MockMultipartFile filePart(String name) {
        return new MockMultipartFile(name, name + ".png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
    }

    @DisplayName("Allows teachers to create a Fotografen task with metadata + file multipart")
    @Test
    void createImageVerificationTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createImageVerificationTask(any(), any(), any())).thenReturn(taskWithId(1L));

        String metadata = """
                {"chapterId": 2, "difficulty": "EASY", "taskDescription": "x",
                 "correctCategory": "REAL", "correctJustification": "j",
                 "justifications": ["j"], "correctDescription": "ok", "falseDescription": "no"}
                """;

        mockMvc.perform(multipart("/admin/tasks/fotografen")
                        .file(metadataPart(metadata))
                        .file(filePart("file"))
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskAdminService).createImageVerificationTask(any(), any(), any());
    }

    @DisplayName("Allows teachers to create a Nyhetskvartalet task with two file parts")
    @Test
    void createNewsVerificationTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createNewsVerificationTask(any(), any(), any(), any())).thenReturn(taskWithId(2L));

        String metadata = """
                {"chapterId": 1, "difficulty": "MEDIUM", "taskDescription": "x",
                 "correctFileSlot": "FILE_1",
                 "correctDescription": "ok", "incorrectDescription": "no"}
                """;

        mockMvc.perform(multipart("/admin/tasks/nyhetskvartalet")
                        .file(metadataPart(metadata))
                        .file(filePart("file1"))
                        .file(filePart("file2"))
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));

        verify(taskAdminService).createNewsVerificationTask(any(), any(), any(), any());
    }

    @DisplayName("Allows teachers to create a Datasenteret image task with multipart")
    @Test
    void createDatacenterImageTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createDatacenterImageTask(any(), any(), any())).thenReturn(taskWithId(3L));

        String metadata = """
                {"taskDescription": "x", "correctCategory": "AI_GENERATED", "correctJustification": "j",
                 "justifications": ["j"], "correctDescription": "ok", "falseDescription": "no"}
                """;

        mockMvc.perform(multipart("/admin/tasks/datasenteret/image")
                        .file(metadataPart(metadata))
                        .file(filePart("file"))
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));

        verify(taskAdminService).createDatacenterImageTask(any(), any(), any());
    }

    @DisplayName("Allows teachers to create a Datasenteret news task with multipart")
    @Test
    void createDatacenterNewsTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createDatacenterNewsTask(any(), any(), any(), any())).thenReturn(taskWithId(4L));

        String metadata = """
                {"taskDescription": "x", "correctFileSlot": "FILE_1",
                 "correctDescription": "ok", "incorrectDescription": "no"}
                """;

        mockMvc.perform(multipart("/admin/tasks/datasenteret/news")
                        .file(metadataPart(metadata))
                        .file(filePart("file1"))
                        .file(filePart("file2"))
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));

        verify(taskAdminService).createDatacenterNewsTask(any(), any(), any(), any());
    }

    @DisplayName("Allows teachers to update a Fotografen task without sending a file")
    @Test
    void updateImageVerificationTask_whenTeacher_returnsOk() throws Exception {
        when(taskAdminService.updateImageVerificationTask(any(), any(), any(), any()))
                .thenReturn(taskWithId(1L));

        String metadata = """
                {"difficulty": "EASY", "taskDescription": "x", "correctCategory": "REAL",
                 "correctJustification": "j", "justifications": ["j"],
                 "correctDescription": "ok", "falseDescription": "no"}
                """;

        mockMvc.perform(multipart(HttpMethod.PATCH, "/admin/tasks/fotografen/1")
                        .file(metadataPart(metadata))
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isOk());

        verify(taskAdminService).updateImageVerificationTask(any(), any(), any(), any());
    }

    @DisplayName("Allows teachers to update a Nyhetskvartalet task with both file replacements")
    @Test
    void updateNewsVerificationTask_whenTeacher_returnsOk() throws Exception {
        when(taskAdminService.updateNewsVerificationTask(any(), any(), any(), any(), any()))
                .thenReturn(taskWithId(2L));

        String metadata = """
                {"difficulty": "HARD", "taskDescription": "x", "correctFileSlot": "FILE_2",
                 "correctDescription": "ok", "incorrectDescription": "no"}
                """;

        mockMvc.perform(multipart(HttpMethod.PATCH, "/admin/tasks/nyhetskvartalet/2")
                        .file(metadataPart(metadata))
                        .file(filePart("file1"))
                        .file(filePart("file2"))
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isOk());

        verify(taskAdminService).updateNewsVerificationTask(any(), any(), any(), any(), any());
    }

    @DisplayName("Denies pupils from any /admin/tasks endpoint")
    @Test
    void anyAdminEndpoint_whenPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/admin/tasks/fotografen")
                        .param("classroomCode", "ABCABC")
                        .with(authentication(authFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(taskAdminService);
    }

    @DisplayName("Denies unauthenticated requests to /admin/tasks endpoints")
    @Test
    void anyAdminEndpoint_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/tasks/fotografen").param("classroomCode", "ABCABC"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskAdminService);
    }

    @DisplayName("Allows teachers to list Fotografen tasks scoped to a classroom")
    @Test
    void listFotografenTasks_whenTeacher_returnsTasks() throws Exception {
        when(taskAdminService.listFotografenTasksForClassroom("teacher@example.com", "ABCABC"))
                .thenReturn(List.of(taskWithId(1L), taskWithId(2L)));

        mockMvc.perform(get("/admin/tasks/fotografen")
                        .param("classroomCode", "ABCABC")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(taskAdminService).listFotografenTasksForClassroom("teacher@example.com", "ABCABC");
    }

    @DisplayName("Allows teachers to list Nyhetskvartalet tasks scoped to a classroom")
    @Test
    void listNyhetskvartaletTasks_whenTeacher_returnsTasks() throws Exception {
        when(taskAdminService.listNyhetskvartaletTasksForClassroom("teacher@example.com", "ABCABC"))
                .thenReturn(List.of(taskWithId(7L)));

        mockMvc.perform(get("/admin/tasks/nyhetskvartalet")
                        .param("classroomCode", "ABCABC")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));

        verify(taskAdminService).listNyhetskvartaletTasksForClassroom("teacher@example.com", "ABCABC");
    }

    @DisplayName("Allows teachers to list Datasenteret tasks scoped to a classroom")
    @Test
    void listDatasenteretTasks_whenTeacher_returnsTasks() throws Exception {
        when(taskAdminService.listDatasenteretTasksForClassroom("teacher@example.com", "ABCABC"))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/tasks/datasenteret")
                        .param("classroomCode", "ABCABC")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(taskAdminService).listDatasenteretTasksForClassroom("teacher@example.com", "ABCABC");
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
    }

    private Authentication authFor(JwtUserPrincipal principal, String authority) {
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
    }

    private TaskAdminResponse taskWithId(Long id) {
        return new TaskAdminResponse(id, TaskDifficulty.EASY, 1L, null, "{}", null);
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
