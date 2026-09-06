package com.marketpulse.backend.watchlist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    @EntityGraph(attributePaths = "items")
    List<Watchlist> findAllByUserEmailOrderByUpdatedAtDesc(String email);

    @EntityGraph(attributePaths = "items")
    Optional<Watchlist> findByIdAndUserEmail(Long id, String email);

    Optional<Watchlist> findByUserEmailAndName(String email, String name);
}