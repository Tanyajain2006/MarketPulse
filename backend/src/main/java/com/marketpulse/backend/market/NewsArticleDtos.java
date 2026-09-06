package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;

public final class NewsArticleDtos {
    private NewsArticleDtos() { }

    public record ArticleResponse(Long id, String ticker, String headline, String source,
            Instant publishedAt, Instant ingestionTimestamp, NewsEventType eventType,
            BigDecimal sentimentScore) { }
}