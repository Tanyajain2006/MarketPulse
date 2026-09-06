package com.marketpulse.backend.preferences;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.preferences.PreferenceDtos.PreferenceResponse;
import com.marketpulse.backend.preferences.PreferenceDtos.UpdatePreferenceRequest;
import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;

@Service
public class PreferenceService {
    private final UserRepository users;
    private final UserPreferenceRepository preferences;

    public PreferenceService(UserRepository users, UserPreferenceRepository preferences) {
        this.users = users;
        this.preferences = preferences;
    }

    @Transactional
    public PreferenceResponse get(String email) {
        User user = user(email);
        UserPreference preference = preferences.findByUserEmail(user.getEmail())
                .orElseGet(() -> preferences.save(new UserPreference(user)));
        return response(user, preference);
    }

    @Transactional
    public PreferenceResponse update(String email, UpdatePreferenceRequest request) {
        User user = user(email);
        UserPreference preference = preferences.findByUserEmail(user.getEmail())
                .orElseGet(() -> new UserPreference(user));
        if (request.displayName() != null && !request.displayName().isBlank()) user.rename(request.displayName().trim());
        if (request.readingDensity() != null) preference.setReadingDensity(request.readingDensity());
        return response(user, preferences.save(preference));
    }

    private User user(String email) { return users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found.")); }
    private PreferenceResponse response(User user, UserPreference preference) {
        return new PreferenceResponse(user.getId(), user.getName(), user.getEmail(), preference.getReadingDensity(), preference.getUpdatedAt());
    }
}
