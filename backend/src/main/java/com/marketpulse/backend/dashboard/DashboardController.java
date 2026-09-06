package com.marketpulse.backend.dashboard;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/overview")
    public DashboardDtos.Overview overview(Authentication authentication) { return service.overview(authentication.getName()); }

    @PostMapping("/checkpoint")
    @ResponseStatus(HttpStatus.OK)
    public Instant checkpoint(Authentication authentication) { return service.checkpoint(authentication.getName()); }
}
