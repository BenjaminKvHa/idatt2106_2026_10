package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.nettdet.backend.domain.Task;
import com.nettdet.backend.dto.FileReferenceResponse;
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

    @DisplayName("Allows teachers to look up an existing file reference and returns 200")
    @Test
    void getOrCreateFileReference_whenFileExists_returnsOkWithReference() throws Exception {
        when(taskAdminService.getOrCreateFileReference("k", "image/png", "x.png"))
                .thenReturn(new FileReferenceResponse(42L, "k", false));

        mockMvc.perform(get("/admin/tasks/files/reference")
                        .param("objectKey", "k")
                        .param("contentType", "image/png")
                        .param("fileName", "x.png")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(42))
                .andExpect(jsonPath("$.newlyCreated").value(false));

        verify(taskAdminService).getOrCreateFileReference("k", "image/png", "x.png");
    }

    @DisplayName("Returns 201 when the file reference was newly created")
    @Test
    void getOrCreateFileReference_whenNewlyCreated_returnsCreated() throws Exception {
        when(taskAdminService.getOrCreateFileReference("k", "image/png", "x.png"))
                .thenReturn(new FileReferenceResponse(42L, "k", true));

        mockMvc.perform(get("/admin/tasks/files/reference")
                        .param("objectKey", "k")
                        .param("contentType", "image/png")
                        .param("fileName", "x.png")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newlyCreated").value(true));
    }

    @DisplayName("Allows teachers to create a Fotografen task")
    @Test
    void createImageVerificationTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createImageVerificationTask(any())).thenReturn(taskWithId(1L));

        String body = """
                {"chapterId": 2, "difficulty": "EASY", "taskDescription": "x",
                 "fileId": 1, "correctCategory": "REAL", "correctJustification": "j",
                 "justifications": ["j"], "correctDescription": "ok", "falseDescription": "no"}
                """;

        mockMvc.perform(post("/admin/tasks/fotografen")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskAdminService).createImageVerificationTask(any());
    }

    @DisplayName("Allows teachers to create a Nyhetskvartalet task")
    @Test
    void createNewsVerificationTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createNewsVerificationTask(any())).thenReturn(taskWithId(2L));

        String body = """
                {"chapterId": 1, "difficulty": "MEDIUM", "taskDescription": "x",
                 "fileId_1": 10, "fileId_2": 20, "correctFileId": 10,
                 "correctDescription": "ok", "incorrectDescription": "no"}
                """;

        mockMvc.perform(post("/admin/tasks/nyhetskvartalet")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));

        verify(taskAdminService).createNewsVerificationTask(any());
    }

    @DisplayName("Allows teachers to create a Datasenteret image task")
    @Test
    void createDatacenterImageTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createDatacenterImageTask(any())).thenReturn(taskWithId(3L));

        String body = """
                {"taskDescription": "x", "fileId": 7,
                 "correctCategory": "AI_GENERATED", "correctJustification": "j",
                 "justifications": ["j"], "correctDescription": "ok", "falseDescription": "no"}
                """;

        mockMvc.perform(post("/admin/tasks/datasenteret/image")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));

        verify(taskAdminService).createDatacenterImageTask(any());
    }

    @DisplayName("Allows teachers to create a Datasenteret news task")
    @Test
    void createDatacenterNewsTask_whenTeacher_returnsCreated() throws Exception {
        when(taskAdminService.createDatacenterNewsTask(any())).thenReturn(taskWithId(4L));

        String body = """
                {"taskDescription": "x", "fileId_1": 10, "fileId_2": 20,
                 "correctFileId": 10, "correctDescription": "ok", "incorrectDescription": "no"}
                """;

        mockMvc.perform(post("/admin/tasks/datasenteret/news")
                        .with(authentication(authFor(teacherPrincipal(), "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));

        verify(taskAdminService).createDatacenterNewsTask(any());
    }

    @DisplayName("Denies pupils from any /admin/tasks endpoint")
    @Test
    void anyAdminEndpoint_whenPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/admin/tasks/files/reference")
                        .param("objectKey", "k")
                        .param("contentType", "image/png")
                        .param("fileName", "x.png")
                        .with(authentication(authFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(taskAdminService);
    }

    @DisplayName("Denies unauthenticated requests to /admin/tasks endpoints")
    @Test
    void anyAdminEndpoint_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/tasks/files/reference")
                        .param("objectKey", "k")
                        .param("contentType", "image/png")
                        .param("fileName", "x.png"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(taskAdminService);
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
    }

    private Authentication authFor(JwtUserPrincipal principal, String authority) {
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
    }

    private Task taskWithId(Long id) {
        Task task = new Task();
        task.setId(id);
        return task;
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
