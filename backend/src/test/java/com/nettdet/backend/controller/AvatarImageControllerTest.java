package com.nettdet.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.service.AvatarImageService;
import com.nettdet.backend.service.FileObjectService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = AvatarImageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(AvatarImageControllerTest.TestSecurityConfig.class)
class AvatarImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvatarImageService avatarImageService;

    @DisplayName("Returns the avatar image bytes with content-type and inline filename header")
    @Test
    void getAvatarImage_returnsBytesAndHeaders() throws Exception {
        FileObject fileObject = new FileObject();
        fileObject.setObjectKey("avatar-images/p_h_c.png");
        fileObject.setFileName("p_h_c.png");
        FileObjectService.StoredFile stored = new FileObjectService.StoredFile(
                fileObject, "image/png", new byte[] {7, 8, 9});
        when(avatarImageService.getAvatarImage("p_h_c.png")).thenReturn(stored);

        mockMvc.perform(get("/avatar-images/p_h_c.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("p_h_c.png")))
                .andExpect(content().bytes(new byte[] {7, 8, 9}));

        verify(avatarImageService).getAvatarImage("p_h_c.png");
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
}
