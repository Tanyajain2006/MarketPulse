package com.marketpulse.backend.market;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsArticleImportServiceTest {
    @Mock private NewsProvider provider;
    @Mock private NewsArticleRepository articles;

    @Test
    void skipsDuplicateArticleAndLeavesSentimentNull() throws Exception {
        NewsDataBatch.NewsDataPoint row = new NewsDataBatch.NewsDataPoint("AAPL", "Results", "Wire",
                Instant.parse("2026-09-01T10:00:00Z"), NewsEventType.EARNINGS);
        when(provider.load(Path.of("news.csv"))).thenReturn(new NewsDataBatch(List.of(row), 1, 0));
        when(articles.existsByTickerAndPublishedAtAndHeadlineAndSource("AAPL", row.publishedAt(), row.headline(), row.source()))
                .thenReturn(true);

        NewsArticleImportService.ImportSummary summary = new NewsArticleImportService(provider, articles, "news.csv")
                .importConfiguredFile();

        assertThat(summary.importedRows()).isZero();
        assertThat(summary.duplicateRows()).isEqualTo(1);
        org.mockito.Mockito.verify(articles, org.mockito.Mockito.never()).save(any(NewsArticle.class));
    }
}