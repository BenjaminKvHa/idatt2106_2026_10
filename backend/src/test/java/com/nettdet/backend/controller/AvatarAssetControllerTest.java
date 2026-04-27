package com.nettdet.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.dto.AvatarAssetResponse;
import com.nettdet.backend.service.AvatarAssetService;
import com.nettdet.backend.service.FileObjectService;
import com.nettdet.backend.util.JwtAuthFilter;

@WebMvcTest(
        controllers = AvatarAssetController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import(AvatarAssetControllerTest.TestSecurityConfig.class)
class AvatarAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvatarAssetService avatarAssetService;

    @DisplayName("Returns the list of avatar assets as JSON")
    @Test
    void getAssets_returnsAssetListJson() throws Exception {
        when(avatarAssetService.getAllAssets()).thenReturn(List.of(
                new AvatarAssetResponse("hair", "hair1", "png"),
                new AvatarAssetResponse("person", "p1", "png")));

        mockMvc.perform(get("/avatar-assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].assetType").value("hair"))
                .andExpect(jsonPath("$[0].name").value("hair1"))
                .andExpect(jsonPath("$[1].assetType").value("person"));

        verify(avatarAssetService).getAllAssets();
    }

    @DisplayName("Returns the asset image bytes with content-type and inline filename header")
    @Test
    void getAsset_returnsBytesAndHeaders() throws Exception {
        FileObject fileObject = new FileObject();
        fileObject.setObjectKey("avatar-assets/hair/hair1.png");
        fileObject.setFileName("hair1.png");
        FileObjectService.StoredFile stored = new FileObjectService.StoredFile(
                fileObject, "image/png", new byte[] {1, 2, 3});
        when(avatarAssetService.getAsset("hair", "hair1.png")).thenReturn(stored);

        mockMvc.perform(get("/avatar-assets/hair/hair1.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("hair1.png")))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));

        verify(avatarAssetService).getAsset("hair", "hair1.png");
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
