package com.nettdet.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;

/**
 * {@code OpenApiConfig} provides the configuration for OpenAPI 3.0 / Swagger documentation.
 * <p>
 * This class defines the global API metadata, contact information, and security schemes
 * required to interact with the backend services. Specifically, it configures a JWT-based
 * Bearer authentication scheme, allowing developers to test protected endpoints directly
 * through the Swagger UI.
 * </p>
 * <p>
 * The documentation covers a wide range of domains including authentication, classroom
 * management, pupil progress, gamification (leaderboards, medals), and asset management.
 * </p>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Nettdet API",
                version = "v1",
                description = "OpenAPI documentation for authentication, classroom, pupil, leaderboard, notebook, tips, avatar, and file endpoints.",
                contact = @Contact(name = "Nettdet Backend")))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

    /**
     * Creates and configures the {@link OpenAPI} bean for the application.
     * <p>
     * This bean supplements the {@code @OpenAPIDefinition} with programmatic configuration,
     * including license details and a centralized title and version.
     * </p>
     *
     * @return a fully configured {@code OpenAPI} instance
     */
    @Bean
    public OpenAPI nettdetOpenApi() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Nettdet API")
                        .version("v1")
                        .description("Interactive API documentation for the Nettdet backend.")
                        .license(new License().name("Internal project use")));
    }
}
