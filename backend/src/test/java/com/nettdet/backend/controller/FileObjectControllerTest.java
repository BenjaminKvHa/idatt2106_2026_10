package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.FileObjectAccessControlService;
import com.nettdet.backend.service.FileObjectService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = FileObjectController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(FileObjectControllerTest.TestSecurityConfig.class)
class FileObjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileObjectAccessControlService fileObjectAccessControlService;

    @DisplayName("Read returns the file bytes with content-type and inline filename header")
    @Test
    void read_whenAuthenticated_returnsBytesAndHeaders() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        FileObject fileObject = fileObject(7L, "image.png");
        FileObjectService.StoredFile stored = new FileObjectService.StoredFile(
                fileObject, "image/png", new byte[] {1, 2, 3});
        when(fileObjectAccessControlService.read(eq(7L), any())).thenReturn(stored);

        mockMvc.perform(get("/objects/7")
                        .with(authentication(authFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("image.png")))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));

        verify(fileObjectAccessControlService).read(eq(7L), any());
    }

    @DisplayName("Delete returns 204 when the access-control service permits the deletion")
    @Test
    void delete_whenAuthenticated_returnsNoContent() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();

        mockMvc.perform(delete("/objects/8")
                        .with(authentication(authFor(principal, "PUPIL"))))
                .andExpect(status().isNoContent());

        verify(fileObjectAccessControlService).delete(eq(8L), any());
    }

    @DisplayName("Upload returns 201 with the new file id")
    @Test
    void upload_whenAuthenticated_returnsCreatedWithFileId() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        FileObject stored = fileObject(99L, "x.png");
        when(fileObjectAccessControlService.upload(any(), eq("ABCABC"), eq(ClassroomAccess.ONLY_CLASS), any()))
                .thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/objects")
                        .file(file)
                        .param("classroomCode", "ABCABC")
                        .param("accessLevel", "ONLY_CLASS")
                        .with(authentication(authFor(principal, "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(content().string("99"));

        verify(fileObjectAccessControlService).upload(any(), eq("ABCABC"), eq(ClassroomAccess.ONLY_CLASS), any());
    }

    @DisplayName("Upload defaults the access level to PUBLIC when omitted")
    @Test
    void upload_whenAccessLevelOmitted_defaultsToPublic() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        FileObject stored = fileObject(100L, "x.png");
        when(fileObjectAccessControlService.upload(any(), any(), eq(ClassroomAccess.PUBLIC), any()))
                .thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart("/objects")
                        .file(file)
                        .with(authentication(authFor(principal, "TEACHER"))))
                .andExpect(status().isCreated())
                .andExpect(content().string("100"));

        verify(fileObjectAccessControlService).upload(any(), any(), eq(ClassroomAccess.PUBLIC), any());
    }

    @DisplayName("Read denies unauthenticated requests")
    @Test
    void read_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/objects/7"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(fileObjectAccessControlService);
    }

    @DisplayName("Delete denies unauthenticated requests")
    @Test
    void delete_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/objects/8"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(fileObjectAccessControlService);
    }

    @DisplayName("Upload denies unauthenticated requests")
    @Test
    void upload_whenUnauthenticated_returnsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart("/objects").file(file))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(fileObjectAccessControlService);
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
    }

    private Authentication authFor(JwtUserPrincipal principal, String authority) {
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
    }

    private FileObject fileObject(Long id, String name) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey(name);
        fileObject.setFileName(name);
        fileObject.setAccessLevel(ClassroomAccess.PUBLIC);
        return fileObject;
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
