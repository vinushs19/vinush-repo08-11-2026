package com.college.resourcefinder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for CORS, frontend static resource mapping, and API routing.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure static resource locations to serve the frontend foundation.
     * Serves from both packaged classpath:/static/ and root ../frontend/ directory.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/static/",
                        "file:../frontend/",
                        "file:./frontend/"
                );
    }

    /**
     * Cross-Origin Resource Sharing (CORS) configuration for local development.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080", "http://127.0.0.1:8080", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
