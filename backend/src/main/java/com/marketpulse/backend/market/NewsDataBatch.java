package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

public record NewsDataBatch(List<NewsDataPoint> rows, int totalRows, int rejectedRows) {
    public record NewsDataPoint(String ticker, String headline, String source,
            Instant publishedAt, NewsEventType eventType) { }
}