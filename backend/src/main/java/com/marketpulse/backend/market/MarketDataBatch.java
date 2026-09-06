package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MarketDataBatch(List<MarketDataPoint> rows, int totalRows, int rejectedRows) {
    public record MarketDataPoint(String ticker, Instant observationTimestamp, BigDecimal price, Long volume,
            BigDecimal volatility, BigDecimal sectorChange, String source) { }
}