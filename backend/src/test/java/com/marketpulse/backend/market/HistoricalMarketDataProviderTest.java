package com.marketpulse.backend.market;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class HistoricalMarketDataProviderTest {
    private final HistoricalMarketDataProvider provider = new HistoricalMarketDataProvider();

    @Test
    void loadsValidRowsAndPreservesObservationTimestamp() throws Exception {
        Path file = Files.createTempFile("market-data", ".csv");
        Files.writeString(file, "timestamp,ticker,price,volume,volatility,sector_change\n"
                + "2026-09-01T09:30:00,AAPL,229.45,1200345,0.021,0.84\n"
                + "2026-09-01T10:00:00,AAPL,230.10,1312450,0.019,0.67\n");

        MarketDataBatch result = provider.load(file);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.rejectedRows()).isZero();
        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).observationTimestamp().toString()).isEqualTo("2026-09-01T09:30:00Z");
        assertThat(result.rows().get(0).ticker()).isEqualTo("AAPL");
    }

    @Test
    void rejectsMalformedTimestampAndNumericRowsWithoutStoppingImport() throws Exception {
        Path file = Files.createTempFile("market-data", ".csv");
        Files.writeString(file, "timestamp,ticker,price,volume,volatility,sector_change\n"
                + "not-a-time,AAPL,229.45,1200345,0.021,0.84\n"
                + "2026-09-01T10:00:00,NVDA,not-a-number,1312450,0.019,0.67\n"
                + "2026-09-01T11:00:00,NVDA,175.44,3580102,0.031,-0.44\n");

        MarketDataBatch result = provider.load(file);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.rejectedRows()).isEqualTo(2);
        assertThat(result.rows()).singleElement().extracting(MarketDataBatch.MarketDataPoint::ticker).isEqualTo("NVDA");
    }
}