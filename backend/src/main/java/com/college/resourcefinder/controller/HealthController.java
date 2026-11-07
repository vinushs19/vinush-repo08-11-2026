package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health-check controller providing application status and environment diagnostics.
 * Exposed at {@code /api/v1/health}.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Value("${spring.application.name:college-resource-finder}")
    private String applicationName;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("application", applicationName);
        details.put("status", "UP");
        details.put("version", "1.0.0-SNAPSHOT");
        details.put("framework", "Spring Boot 3.4.1");
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("timestamp", Instant.now().toString());

        return ApiResponse.success("College Resource Finder API is healthy and operational.", details);
    }
}
