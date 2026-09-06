package com.marketpulse.backend.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() { }

    public record Summary(int needsAttention, int worthWatching, int noMaterialChange, int unacknowledged) { }

    public record Change(String ticker, String companyName, String exchange, Instant observedAt,
            BigDecimal price, BigDecimal priceChangePercent, Long volume, BigDecimal volumeMultiple,
            BigDecimal volatility, BigDecimal sectorChange, String classification, int materiality,
            String headline, String eventType, BigDecimal sentimentScore) { }

    public record Overview(String watchlistName, Instant checkpoint, Instant latestObservation, Summary summary,
            List<Change> changes) { }
}
