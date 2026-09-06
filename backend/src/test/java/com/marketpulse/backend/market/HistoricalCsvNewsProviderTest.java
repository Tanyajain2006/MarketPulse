package com.marketpulse.backend.market;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class HistoricalCsvNewsProviderTest {
    private final HistoricalCsvNewsProvider provider = new HistoricalCsvNewsProvider();

    @Test
    void loadsValidArticlesWithNullableSentimentInputIgnored() throws Exception {
        Path file = Files.createTempFile("news", ".csv");
        Files.writeString(file, "timestamp,ticker,headline,source,event_type,sentiment_score\n"
                + "2026-09-01T09:30:00,AAPL,Results published,Wire, EARNINGS,0.8\n"
                + "2026-09-01T10:00:00,NVDA,Product update,Wire,PRODUCT_NEWS,0.4\n");

        NewsDataBatch result = provider.load(file);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.rejectedRows()).isZero();
        assertThat(result.rows()).extracting(NewsDataBatch.NewsDataPoint::ticker).containsExactly("AAPL", "NVDA");
        assertThat(result.rows().get(0).eventType()).isEqualTo(NewsEventType.EARNINGS);
    }

    @Test
    void rejectsBadRowsButContinuesValidRows() throws Exception {
        Path file = Files.createTempFile("news", ".csv");
        Files.writeString(file, "timestamp,ticker,headline,source,event_type\n"
                + "bad-time,AAPL,Headline,Wire,EARNINGS\n"
                + "2026-09-01T10:00:00,,Headline,Wire,EARNINGS\n"
                + "2026-09-01T11:00:00,NVDA,Valid,Wire,COMPANY_NEWS\n");

        NewsDataBatch result = provider.load(file);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.rejectedRows()).isEqualTo(2);
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.ticker()).isEqualTo("NVDA");
            assertThat(row.eventType()).isEqualTo(NewsEventType.OTHER);
        });
    }
}