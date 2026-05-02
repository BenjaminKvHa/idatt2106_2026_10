package com.nettdet.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("Passes through without authenticating when no Authorization header is present")
    @Test
    void doFilter_whenAuthorizationHeaderMissing_continuesChainWithoutSettingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @DisplayName("Passes through without authenticating when header lacks the Bearer prefix")
    @Test
    void doFilter_whenAuthorizationHeaderHasNoBearerPrefix_continuesChainWithoutSettingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @DisplayName("Returns 401 and halts the chain when the Bearer token is invalid")
    @Test
    void doFilter_whenTokenInvalid_returns401AndDoesNotAdvanceChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @DisplayName("Returns 401 when required JWT claims are missing")
    @Test
    void doFilter_whenRequiredClaimsMissing_returns401AndDoesNotAdvanceChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractEmail("token")).thenReturn(null);
        when(jwtUtil.extractUserRole("token")).thenReturn("PUPIL");
        when(jwtUtil.extractLegalName("token")).thenReturn("Pupil Name");

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @DisplayName("Sets the SecurityContext with the JWT principal and role authority when the token is valid")
    @Test
    void doFilter_whenTokenValid_setsAuthenticationWithPrincipalAndAuthorityAndAdvancesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.isTokenValid("good-token")).thenReturn(true);
        when(jwtUtil.extractEmail("good-token")).thenReturn("pupil@example.com");
        when(jwtUtil.extractUserRole("good-token")).thenReturn("PUPIL");
        when(jwtUtil.extractLegalName("good-token")).thenReturn("Pupil Name");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getPrincipal() instanceof JwtUserPrincipal);
        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();
        assertEquals("pupil@example.com", principal.email());
        assertEquals("Pupil Name", principal.legalName());
        assertEquals("PUPIL", principal.role());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "PUPIL".equals(a.getAuthority())));
    }

    @DisplayName("Sets the SecurityContext with no authorities when the role claim is blank")
    @Test
    void doFilter_whenRoleIsBlank_setsAuthenticationWithEmptyAuthorities() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.isTokenValid("good-token")).thenReturn(true);
        when(jwtUtil.extractEmail("good-token")).thenReturn("pupil@example.com");
        when(jwtUtil.extractUserRole("good-token")).thenReturn("");
        when(jwtUtil.extractLegalName("good-token")).thenReturn("Pupil Name");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());
        verify(filterChain).doFilter(request, response);
    }
}
