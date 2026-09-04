package com.marketpulse.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name:marketpulse-backend}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName);
    }

    public record HealthResponse(String status, String service) {
    }
}
