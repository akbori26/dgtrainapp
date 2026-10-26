package com.akbori.dgtrainapp.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets front ends served from another origin (a dev server, a deployed site) call the API,
 * and names the API in Swagger UI.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Dragon Trainer API", version = "1.0.0",
        description = "Play Dragons of Mugloar from any front end. Errors use ProblemDetail with a \"code\" field."))
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${dragontrainapp.cors.allowed-origins:}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) return;
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST");
    }
}
