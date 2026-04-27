package com.nettdet.backend.controller;

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

import com.nettdet.backend.dto.ChapterTipResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.TipService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = TipController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(TipControllerTest.TestSecurityConfig.class)
class TipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TipService tipService;

    @DisplayName("Allows authenticated pupils to fetch a chapter tip")
    @Test
    void getPupilTip_whenAuthenticatedPupil_returnsTipJson() throws Exception {
        JwtUserPrincipal principal = pupilPrincipal();
        when(tipService.getPupilTip(1L, principal))
                .thenReturn(new ChapterTipResponse(
                        1L,
                        "Se nøye på kilden før du bestemmer deg.",
                        true,
                        false));

        mockMvc.perform(get("/pupil/chapters/1/tip")
                        .with(authentication(authenticationFor(principal, "PUPIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(1))
                .andExpect(jsonPath("$.tip").value("Se nøye på kilden før du bestemmer deg."))
                .andExpect(jsonPath("$.newlyAdded").value(true))
                .andExpect(jsonPath("$.allTipsReceived").value(false));

        verify(tipService).getPupilTip(1L, principal);
    }

    @DisplayName("Denies teachers from pupil tip endpoint")
    @Test
    void getPupilTip_whenAuthenticatedTeacher_returnsForbidden() throws Exception {
        mockMvc.perform(get("/pupil/chapters/1/tip")
                        .with(authentication(authenticationFor(teacherPrincipal(), "TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tipService);
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
