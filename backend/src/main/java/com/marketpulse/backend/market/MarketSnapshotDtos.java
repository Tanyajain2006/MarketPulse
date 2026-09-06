package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;

public final class MarketSnapshotDtos {
    private MarketSnapshotDtos() { }

    public record SnapshotResponse(Instant timestamp, String ticker, BigDecimal price, Long volume,
            BigDecimal volatility, BigDecimal sectorChange, String source) { }

    public record NewsResponse(Instant timestamp, String ticker, String headline, String source,
            String eventType, BigDecimal sentimentScore) { }
}
