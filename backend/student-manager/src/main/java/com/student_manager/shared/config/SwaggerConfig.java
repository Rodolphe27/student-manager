package com.student_manager.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc/OpenAPI configuration for the Swagger UI documentation exposed by
 * this API.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Defines the OpenAPI document metadata (title, description, version)
     * shown in Swagger UI.
     *
     * @return the configured {@link OpenAPI} bean
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Manager API")
                        .description("REST API – Vertical Slice Architecture")
                        .version("1.0.0"));
    }
}
