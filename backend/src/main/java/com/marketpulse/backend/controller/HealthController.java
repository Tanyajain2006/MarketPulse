package com.marketpulse.backend.controller;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final String applicationName;
    private final DataSource dataSource;

    public HealthController(@Value("${spring.application.name:marketpulse-backend}") String applicationName,
            DataSource dataSource) {
        this.applicationName = applicationName;
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        try (var connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) return new HealthResponse("DEGRADED", applicationName, "DOWN");
            return new HealthResponse("UP", applicationName, "UP");
        } catch (Exception exception) {
            return new HealthResponse("DEGRADED", applicationName, "DOWN");
        }
    }

    public record HealthResponse(String status, String service, String database) {
    }
}
