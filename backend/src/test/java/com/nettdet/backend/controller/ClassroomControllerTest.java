package com.nettdet.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.dto.AcceptedClassroomPupilResponse;
import com.nettdet.backend.dto.AcceptedPupilMembershipResponse;
import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.ClassroomTeacherResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.PendingPupilRequestResponse;
import com.nettdet.backend.dto.TeacherLeaveStatusResponse;
import com.nettdet.backend.dto.UpdatePupilUsernameRequest;
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
                        "Pupil Name",
                        "detektiv1",
                        "avatar-images/boy_hair1_costume1.png",
                        240L)));

        mockMvc.perform(get("/classrooms/ABCABC/pupils")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountEmail").value("pupil@example.com"))
                .andExpect(jsonPath("$[0].legalName").value("Pupil Name"))
                .andExpect(jsonPath("$[0].username").value("detektiv1"))
                .andExpect(jsonPath("$[0].avatarObjectKey").value("avatar-images/boy_hair1_costume1.png"))
                .andExpect(jsonPath("$[0].totalXp").value(240));

        verify(classroomService).getAcceptedPupils("ABCABC", principal);
    }

    @DisplayName("Allows authenticated teachers to update a pupil classroom username")
    @Test
    void updatePupilUsername_whenAuthenticatedTeacher_returnsUpdatedPupilJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.updatePupilUsername(
                argThat("ABCABC"::equals),
                argThat("pupil@example.com"::equals),
                argThat(request -> request.username().equals("nydetektiv")),
                argThat(argument -> argument.email().equals("teacher@example.com"))))
                .thenReturn(new AcceptedClassroomPupilResponse(
                        "pupil@example.com",
                        "Pupil Name",
                        "nydetektiv",
                        "avatar-images/boy_hair1_costume1.png",
                        240L));

        mockMvc.perform(patch("/classrooms/ABCABC/pupils/pupil@example.com/username")
                        .with(authentication(authenticationFor(principal, "TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePupilUsernameRequest("nydetektiv"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountEmail").value("pupil@example.com"))
                .andExpect(jsonPath("$.username").value("nydetektiv"))
                .andExpect(jsonPath("$.totalXp").value(240));

        verify(classroomService).updatePupilUsername(
                argThat("ABCABC"::equals),
                argThat("pupil@example.com"::equals),
                argThat(request -> request.username().equals("nydetektiv")),
                argThat(argument -> argument.email().equals("teacher@example.com") && argument.role().equals("TEACHER")));
    }

    @DisplayName("Denies pupils from updating another pupil classroom username")
    @Test
    void updatePupilUsername_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(patch("/classrooms/ABCABC/pupils/other@example.com/username")
                        .with(authentication(authenticationFor(principal, "PUPIL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePupilUsernameRequest("nydetektiv"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(classroomService);
    }

    @DisplayName("Allows authenticated teachers to list pending pupil requests with avatar and username")
    @Test
    void getPendingPupilRequests_whenAuthenticatedTeacher_returnsPendingRequestJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.getPendingPupilRequests("ABCABC", principal)).thenReturn(List.of(
                new PendingPupilRequestResponse(
                        "pupil@example.com",
                        "Pupil Name",
                        "detektiv1",
                        "avatar-images/boy_hair1_costume1.png")));

        mockMvc.perform(get("/classrooms/ABCABC/pupils/pending")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountEmail").value("pupil@example.com"))
                .andExpect(jsonPath("$[0].legalName").value("Pupil Name"))
                .andExpect(jsonPath("$[0].username").value("detektiv1"))
                .andExpect(jsonPath("$[0].avatarObjectKey").value("avatar-images/boy_hair1_costume1.png"));

        verify(classroomService).getPendingPupilRequests("ABCABC", principal);
    }

    @DisplayName("Denies pupils from pending pupil request listing")
    @Test
    void getPendingPupilRequests_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/classrooms/ABCABC/pupils/pending")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(classroomService);
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

    @DisplayName("Allows authenticated members to fetch classroom info")
    @Test
    void getClassroom_whenAuthenticated_returnsClassroomInfo() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil", "pupil@example.com", "PUPIL");
        when(classroomService.getClassroom(eq("ABCABC"), any()))
                .thenReturn(new ClassroomResponse("ABCABC", "8A", "Mandag"));

        mockMvc.perform(get("/classrooms/ABCABC")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomCode").value("ABCABC"))
                .andExpect(jsonPath("$.classroomName").value("8A"))
                .andExpect(jsonPath("$.classroomDescription").value("Mandag"));
    }

    @DisplayName("Denies unauthenticated requests to the classroom info endpoint")
    @Test
    void getClassroom_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/classrooms/ABCABC")).andExpect(status().isUnauthorized());

        verifyNoInteractions(classroomService);
    }

    @DisplayName("Allows authenticated teachers to list classroom teachers")
    @Test
    void getClassroomTeachers_whenAuthenticatedTeacher_returnsTeacherListJson() throws Exception {
        JwtUserPrincipal principal = teacherPrincipal();
        when(classroomService.getClassroomTeachers(eq("ABCABC"), any()))
                .thenReturn(List.of(
                        new ClassroomTeacherResponse("a@example.com", "Adam"),
                        new ClassroomTeacherResponse("z@example.com", "Zara")));

        mockMvc.perform(get("/classrooms/ABCABC/teachers")
                        .with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("a@example.com"))
                .andExpect(jsonPath("$[0].legalName").value("Adam"))
                .andExpect(jsonPath("$[1].email").value("z@example.com"));
    }

    @DisplayName("Denies pupils from the classroom teachers endpoint")
    @Test
    void getClassroomTeachers_whenAuthenticatedPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil", "pupil@example.com", "PUPIL");

        mockMvc.perform(get("/classrooms/ABCABC/teachers")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(classroomService);
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
