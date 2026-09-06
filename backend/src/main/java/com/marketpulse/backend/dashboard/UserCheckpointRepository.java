package com.marketpulse.backend.dashboard;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCheckpointRepository extends JpaRepository<UserCheckpoint, Long> {
    Optional<UserCheckpoint> findByUserEmail(String email);
    Optional<UserCheckpoint> findByUserEmailAndWatchlistId(String email, Long watchlistId);
    void deleteByWatchlistId(Long watchlistId);
}
