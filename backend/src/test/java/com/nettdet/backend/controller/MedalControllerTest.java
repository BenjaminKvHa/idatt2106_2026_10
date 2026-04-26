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

import com.nettdet.backend.dto.PupilMedalResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.MedalService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = MedalController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(MedalControllerTest.TestSecurityConfig.class)
class MedalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedalService medalService;

    @DisplayName("Allows authenticated pupils to fetch medals JSON")
    @Test
    void getMedals_whenAuthenticatedPupil_returnsMedalsJson() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", "PUPIL");
        when(medalService.getPupilMedals(principal)).thenReturn(List.of(
                new PupilMedalResponse(
                        "chapter-1",
                        "Nyhetskvartalet",
                        "Fullfør Nyhetskvartalet for å låse opp denne medaljen.",
                        true,
                        12L),
                new PupilMedalResponse(
                        "chapter-2",
                        "Fotografen",
                        "Fullfør Fotografen for å låse opp denne medaljen.",
                        false,
                        null)));

        mockMvc.perform(get("/pupil/medals").with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medalKey").value("chapter-1"))
                .andExpect(jsonPath("$[0].name").value("Nyhetskvartalet"))
                .andExpect(jsonPath("$[0].description").value("Fullfør Nyhetskvartalet for å låse opp denne medaljen."))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[0].imageFileObjectId").value(12))
                .andExpect(jsonPath("$[1].medalKey").value("chapter-2"))
                .andExpect(jsonPath("$[1].unlocked").value(false))
                .andExpect(jsonPath("$[1].imageFileObjectId").doesNotExist());

        verify(medalService).getPupilMedals(argThat(argument ->
                argument.email().equals("pupil@example.com") && argument.role().equals("PUPIL")));
    }

    @DisplayName("Denies authenticated non-pupils from fetching medals")
    @Test
    void getMedals_whenAuthenticatedNonPupil_returnsForbidden() throws Exception {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        mockMvc.perform(get("/pupil/medals").with(authentication(authenticationFor(principal, "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(medalService);
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
