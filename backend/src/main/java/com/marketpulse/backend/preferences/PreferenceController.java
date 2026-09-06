package com.marketpulse.backend.preferences;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketpulse.backend.preferences.PreferenceDtos.PreferenceResponse;
import com.marketpulse.backend.preferences.PreferenceDtos.UpdatePreferenceRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {
    private final PreferenceService service;

    public PreferenceController(PreferenceService service) { this.service = service; }

    @GetMapping
    public PreferenceResponse get(Authentication authentication) { return service.get(authentication.getName()); }

    @PatchMapping
    public PreferenceResponse update(Authentication authentication, @Valid @RequestBody UpdatePreferenceRequest request) {
        return service.update(authentication.getName(), request);
    }
}
