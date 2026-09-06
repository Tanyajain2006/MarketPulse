package com.marketpulse.backend.preferences;

import java.time.Instant;

import jakarta.validation.constraints.Pattern;

public final class PreferenceDtos {
    private PreferenceDtos() { }

    public record PreferenceResponse(Long userId, String displayName, String email, String readingDensity,
            Instant updatedAt) { }

    public record UpdatePreferenceRequest(
            @Pattern(regexp = "COMPACT|COMFORTABLE|EXPANDED", message = "must be COMPACT, COMFORTABLE, or EXPANDED") String readingDensity,
            String displayName) { }
}
