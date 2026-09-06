package com.marketpulse.backend.dashboard;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCheckpointRepository extends JpaRepository<UserCheckpoint, Long> {
    Optional<UserCheckpoint> findByUserEmail(String email);
}
