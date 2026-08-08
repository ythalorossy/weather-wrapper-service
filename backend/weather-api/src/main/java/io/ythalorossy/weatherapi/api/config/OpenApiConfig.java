package io.ythalorossy.weatherapi.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Top-level OpenAPI metadata exposed at {@code /v3/api-docs}.
 *
 * <p>springdoc-openapi auto-discovers {@code @Operation}, {@code @ApiResponse},
 * and {@code @Schema} annotations on controllers and DTOs, so this bean only
 * carries the static "info" block (title, version, description, contact).
 *
 * <p>Auto-exposed endpoints:
 * <ul>
 *   <li>{@code GET /v3/api-docs}         — JSON spec</li>
 *   <li>{@code GET /v3/api-docs.yaml}    — YAML spec</li>
 *   <li>{@code GET /swagger-ui/index.html} — interactive UI</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherWrapperServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather Wrapper Service API")
                        .description("""
                                Spring Boot wrapper around the National Weather Service API with \
                                Redis-backed cache-aside on geocoding (30 d positive, 60 s negative) \
                                and forecasts (12 h). Free-text city input is normalized (trim + \
                                lower-case + whitespace-collapsed) before geocoding via Nominatim.""")
                        .version("v1")
                        .contact(new Contact()
                                .name("Ythalo Rossy Saldanha Lira")
                                .url("https://github.com/ythalorossy"))
                        .license(new License()
                                .name("TBD")
                                .url("https://github.com/ythalorossy/weather-wrapper-service")));
    }
}