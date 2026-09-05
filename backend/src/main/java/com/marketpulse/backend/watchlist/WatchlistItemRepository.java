package com.marketpulse.backend.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    boolean existsByWatchlistIdAndTicker(Long watchlistId, String ticker);
    void deleteByWatchlistIdAndTicker(Long watchlistId, String ticker);
}