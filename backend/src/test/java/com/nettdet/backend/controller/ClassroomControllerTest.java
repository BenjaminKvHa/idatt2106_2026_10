package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.nettdet.backend.dto.AcceptedClassroomPupilResponse;
import com.nettdet.backend.dto.AcceptedPupilMembershipResponse;
import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.TeacherLeaveStatusResponse;
import com.nettdet.backend.dto.ValidateClassroomCodeRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.ClassroomService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = ClassroomController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(ClassroomControllerTest.TestSecurityConfig.class)
class ClassroomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClassroomService classroomService;

    @DisplayName("Allows authenticated teachers to create classrooms")
    @Test
    void createClassroom_whenAuthenticatedTeacher_returnsCreatedClassroom() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.createClassroom(
                argThat(request -> request.name().equals("8A") && request.description().equals("Mandag")),
                argThat(argument -> argument.email().equals("teacher@example.com"))))
                .thenReturn(new ClassroomResponse("ABCABC", "8A", "Mandag"));

        mockMvc.perform(post("/classrooms")
                        .with(authentication(authenticationFor(principal, "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClassroomRequest("8A", "Mandag"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.classroomName").value("8A"))
                .andExpect(jsonPath("$.classroomDescription").value("Mandag"));

        verify(classroomService).createClassroom(
                argThat(request -> request.name().equals("8A") && request.description().equals("Mandag")),
                argThat(argument -> argument.email().equals("teacher@example.com") && argument.role().equals("TEACHER")));
    }

    @DisplayName("Rejects invalid classroom creation payloads before service execution")
    @Test
    void createClassroom_whenNameIsBlank_returnsBadRequest() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();

        mockMvc.perform(post("/classrooms")
                        .with(authentication(authenticationFor(principal, "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClassroomRequest(" ", "Mandag"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classroomService);
    }

    @DisplayName("Denies pupils from creating classrooms")
    @Test
    void createClassroom_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(post("/classrooms")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClassroomRequest("8A", "Mandag"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(classroomService);
    }

    @DisplayName("Allows authenticated pupils to validate classroom codes")
    @Test
    void validateClassroomCode_whenAuthenticatedPupil_returnsCodeExistence() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(classroomService.validateClassroomCode(
                argThat(request -> request.classroomCode().equals("ABCABC")),
                argThat(argument -> argument.email().equals("pupil@example.com"))))
                .thenReturn(new ValidateClassroomCodeResponse(true));

        mockMvc.perform(post("/classrooms/validate-code")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidateClassroomCodeRequest("ABCABC"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(classroomService).validateClassroomCode(
                argThat(request -> request.classroomCode().equals("ABCABC")),
                argThat(argument -> argument.email().equals("pupil@example.com") && argument.role().equals("PUPIL")));
    }

    @DisplayName("Denies teachers from validating classroom codes through the pupil endpoint")
    @Test
    void validateClassroomCode_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();

        mockMvc.perform(post("/classrooms/validate-code")
                        .with(authentication(authenticationFor(principal, "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidateClassroomCodeRequest("ABCABC"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(classroomService);
    }

    @DisplayName("Allows authenticated teachers to list accepted classroom pupils")
    @Test
    void getAcceptedPupils_whenAuthenticatedTeacher_returnsAcceptedPupilJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.getAcceptedPupils("ABCABC", principal)).thenReturn(List.of(
                new AcceptedClassroomPupilResponse(
                        "pupil@example.com",
                        "detektiv1",
                        "avatar-images/boy_hair1_costume1.png")));

        mockMvc.perform(get("/classrooms/ABCABC/pupils")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountEmail").value("pupil@example.com"))
                .andExpect(jsonPath("$[0].username").value("detektiv1"))
                .andExpect(jsonPath("$[0].avatarObjectKey").value("avatar-images/boy_hair1_costume1.png"));

        verify(classroomService).getAcceptedPupils("ABCABC", principal);
    }

    @DisplayName("Allows authenticated teachers to accept pending pupil requests")
    @Test
    void acceptPupilRequest_whenAuthenticatedTeacher_returnsAcceptedMembershipJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.acceptPupilRequest("ABCABC", "pupil@example.com", principal))
                .thenReturn(new AcceptedPupilMembershipResponse("pupil@example.com", "Pupil Name", true));

        mockMvc.perform(put("/classrooms/ABCABC/pupils/pupil@example.com/accept")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountEmail").value("pupil@example.com"))
                .andExpect(jsonPath("$.legalName").value("Pupil Name"))
                .andExpect(jsonPath("$.membershipConfirmedByTeacher").value(true));

        verify(classroomService).acceptPupilRequest("ABCABC", "pupil@example.com", principal);
    }

    @DisplayName("Allows authenticated teachers to check if they can leave a classroom")
    @Test
    void canLeaveClassroom_whenAuthenticatedTeacher_returnsLeaveStatus() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.getTeacherLeaveStatus("ABCABC", principal))
                .thenReturn(new TeacherLeaveStatusResponse(true));

        mockMvc.perform(get("/classrooms/ABCABC/teachers/me/can-leave")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canLeave").value(true));

        verify(classroomService).getTeacherLeaveStatus("ABCABC", principal);
    }

    @DisplayName("Allows authenticated pupils to leave their own classroom membership")
    @Test
    void leaveClassroomAsPupil_whenAuthenticatedPupil_returnsNoContent() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(delete("/classrooms/ABCABC/pupils/me")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isNoContent());

        verify(classroomService).leaveClassroomAsPupil("ABCABC", principal);
    }

    @DisplayName("Denies teachers from using the pupil leave endpoint")
    @Test
    void leaveClassroomAsPupil_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();

        mockMvc.perform(delete("/classrooms/ABCABC/pupils/me")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(classroomService);
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
