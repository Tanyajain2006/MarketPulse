package com.marketpulse.backend.preferences;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUserEmail(String email);
}
