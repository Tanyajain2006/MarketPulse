package com.marketpulse.backend.watchlist;

import java.time.Instant;

public final class CheckpointDtos {
    private CheckpointDtos() { }

    public record CheckpointResponse(Long id, Long watchlistId, Instant reviewedAt) { }
}