package com.marketpulse.backend.preferences;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {
    @Mock private UserRepository users;
    @Mock private UserPreferenceRepository preferences;

    @Test
    void persistsDisplayNameAndReadingDensityForCurrentUser() {
        User user = new User("Ada", "ada@example.com", "hash");
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(preferences.findByUserEmail("ada@example.com")).thenReturn(Optional.empty());
        when(preferences.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PreferenceService service = new PreferenceService(users, preferences);
        PreferenceDtos.PreferenceResponse response = service.update("ada@example.com",
                new PreferenceDtos.UpdatePreferenceRequest("COMPACT", "Ada Lovelace"));

        assertThat(response.displayName()).isEqualTo("Ada Lovelace");
        assertThat(response.readingDensity()).isEqualTo("COMPACT");
        assertThat(response.email()).isEqualTo("ada@example.com");
    }
}