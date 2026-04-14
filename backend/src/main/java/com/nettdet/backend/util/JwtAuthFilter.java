package com.nettdet.backend.util;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nettdet.backend.security.JwtUserPrincipal;

import org.springframework.lang.NonNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Custom security filter responsible for intercepting HTTP requests and validating JWT authentication.
 * <p>
 * This filter extends {@link OncePerRequestFilter} to ensure execution exactly once per request.
 * It extracts the Bearer token from the {@code Authorization} header, validates its integrity
 * and expiration via {@link JwtUtil}, and populates the {@link SecurityContextHolder} with
 * a {@link JwtUserPrinciple} if the token is valid.
 * </p>
 * <p>
 * If authentication fails or required claims are missing, it sends an {@code SC_UNAUTHORIZED}
 * response and halts the filter chain.
 * </p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Intercepts the request to perform JWT validation and establish the security context.
     *
     * @param request     The incoming {@link HttpServletRequest}.
     * @param response    The outgoing {@link HttpServletResponse}.
     * @param filterChain The {@link FilterChain} to provide the request to the next security layer.
     * @throws ServletException if a servlet-related error occurs.
     * @throws IOException      if an I/O error occurs during processing.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            String jwtToken = token.substring(7); // starts the string at index 7 of the original string

            if (!jwtUtil.isTokenValid(jwtToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.error("Invalid JWT Token, Unauthorized to resume further");
                return;
            }


            String email = jwtUtil.extractEmail(jwtToken);
            String role = jwtUtil.extractUserRole(jwtToken);
            String legalName = jwtUtil.extractLegalName(jwtToken);
            

            if (email == null || legalName == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.error("Missing required claims in JWT token");
                return;
            }

            JwtUserPrincipal principle = new JwtUserPrincipal(legalName, email, role);

            // Class in spring security that represents an authentication object (in this case user)
            List<SimpleGrantedAuthority> authorities =
                role != null && !role.isBlank()
                    ? List.of(new SimpleGrantedAuthority(role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principle, null, authorities);

            // SecurityContextHolder is a global context storage, storing the authenticated entity for use in the filterchain
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("token validated, pass response and request to next filter in chain");
        }

        filterChain.doFilter(request, response); //Pass this custom filter to next filter in filterchain
    }
}
