package com.nettdet.backend.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.nettdet.backend.util.JwtAuthFilter;


/**
 * Main security configuration class for the Spring Boot application.
 * <p>
 * This class defines the security filter chain, which dictates how the application handles 
 * authentication, authorization, CORS, and CSRF protection. It leverages a stateless 
 * session policy suitable for JWT-based architectures and integrates the custom 
 * {@link JwtAuthFilter} into the standard Spring Security pipeline.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Configures the {@link SecurityFilterChain} to define the application's security perimeter.
     * <p>
     * Key configurations include:
     * <ul>
     * <li><b>Stateless Sessions:</b> Disables HTTP sessions to ensure every request is independently authenticated via JWT.</li>
     * <li><b>Endpoint Authorization:</b> Permitting public access to authentication, health, and documentation endpoints while securing all others.</li>
     * <li><b>CORS Policy:</b> Explicitly allowing cross-origin requests from trusted local development environments.</li>
     * <li><b>Custom Filter Placement:</b> Injecting {@link JwtAuthFilter} prior to the {@link UsernamePasswordAuthenticationFilter}.</li>
     * <li><b>CSRF Protection:</b> Disabled, as JWTs are inherently protected against CSRF when stored correctly and state is not maintained via sessions.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to build the security configuration.
     * @return the built {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during the security configuration process.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // Since we are using a Jwt token, sessions need to be turned off, since Jwt tokens don't rely on sessions like cookies
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // expose endpoint /auth/login - /auth/register - /auth/refresh - /health, as available to all, but any other requests to other endpoints require authentication
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/signup", "/auth/refresh", "/swagger-ui/**", "/v3/api-docs/**", "/objects/**").permitAll() // for this url endpoint, all requests are permitted
                .anyRequest().authenticated()); // to any other requests to endpoints, authentication is needed

        // since SecurityConfig is initially run before Server Dispatchlet, it blocks due to CORS restriction, therefore we have
        // to define here that our backend can take in requests from the frontend
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(
                "http://localhost:80",
                "http://localhost:8080",
                "http://127.0.0.1:80",
                "http://127.0.0.1:8080"
            ));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
            config.setAllowedHeaders(List.of("*"));
            return config;
        }));

        // add jwtAuthFilter class before the UsernamePassword filter to authenticate the user and set it in the SecurityContextHolder
        // in order for the UsernamePassword filter to know the user is authenticated
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
}
