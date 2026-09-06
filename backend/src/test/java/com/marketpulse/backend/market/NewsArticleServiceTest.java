package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NewsArticleServiceTest {
    @Mock private NewsArticleRepository articles;
    @Mock private MarketService market;

    @Test
    void filtersByTickerAndPublishedRangeChronologically() {
        Instant from = Instant.parse("2026-09-01T09:00:00Z");
        Instant to = Instant.parse("2026-09-01T11:00:00Z");
        NewsArticle article = new NewsArticle("AAPL", "Results", "Wire", from, Instant.parse("2026-09-06T12:00:00Z"), NewsEventType.EARNINGS);
        when(market.normalizeTicker("aapl")).thenReturn("AAPL");
        when(articles.findByTickerAndPublishedAtBetweenOrderByPublishedAtAsc("AAPL", from, to, PageRequest.of(0, 100)))
                .thenReturn(List.of(article));

        List<NewsArticleDtos.ArticleResponse> result = new NewsArticleService(articles, market).byTicker("aapl", from, to, 100);

        assertThat(result).singleElement().satisfies(value -> {
            assertThat(value.ticker()).isEqualTo("AAPL");
            assertThat(value.publishedAt()).isEqualTo(from);
            assertThat(value.ingestionTimestamp()).isAfter(value.publishedAt());
            assertThat(value.sentimentScore()).isNull();
        });
    }

    @Test
    void rejectsMissingOrReversedRange() {
        when(market.normalizeTicker("AAPL")).thenReturn("AAPL");
        NewsArticleService service = new NewsArticleService(articles, market);

        assertThatThrownBy(() -> service.byTicker("AAPL", null, Instant.now(), 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.byTicker("AAPL", Instant.parse("2026-09-02T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"), 100)).isInstanceOf(IllegalArgumentException.class);
    }
}