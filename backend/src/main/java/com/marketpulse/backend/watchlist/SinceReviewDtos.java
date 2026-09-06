package com.marketpulse.backend.watchlist;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.marketpulse.backend.market.MarketChangeEngine;
import com.marketpulse.backend.market.MaterialityScoreEngine;

public final class SinceReviewDtos {
    private SinceReviewDtos() { }

    public record SinceReviewResponse(Long watchlistId, String watchlistName, Instant reviewedAt,
            Instant latestObservationTimestamp, String marketStatus, String status, Summary summary,
            List<StockChange> stocks) { }

    public record Summary(int criticalCount, int meaningfulCount, int worthWatchingCount, int normalCount) { }

    public record StockChange(String ticker, Instant previousObservationTimestamp, Instant currentObservationTimestamp,
            MarketChangeEngine.Status status, BigDecimal materialityScore,
            MaterialityScoreEngine.Classification classification, String warning,
            MarketChangeEngine.PriceResult price, MarketChangeEngine.VolumeResult volume,
            MarketChangeEngine.VolatilityResult volatility, MarketChangeEngine.RelativeMovementResult relativeMovement,
            MarketChangeEngine.PeerComparisonResult peerComparison,
            List<MaterialityScoreEngine.ComponentContribution> contributions) { }
}