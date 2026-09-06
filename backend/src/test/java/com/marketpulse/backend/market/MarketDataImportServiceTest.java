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

import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class MarketDataImportServiceTest {
    @Mock private MarketDataProvider provider;
    @Mock private MarketSnapshotRepository snapshots;

    @Test
    void skipsExistingObservationAndImportsNewObservation() throws Exception {
        MarketDataBatch.MarketDataPoint existing = point("2026-09-01T09:30:00Z");
        MarketDataBatch.MarketDataPoint fresh = point("2026-09-01T10:00:00Z");
        when(provider.load(Path.of("sample.csv"))).thenReturn(new MarketDataBatch(List.of(existing, fresh), 2, 0));
        when(snapshots.existsByTickerAndObservationTimestamp("AAPL", existing.observationTimestamp())).thenReturn(true);
        when(snapshots.existsByTickerAndObservationTimestamp("AAPL", fresh.observationTimestamp())).thenReturn(false);
        when(snapshots.save(any(MarketSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarketDataImportService service = new MarketDataImportService(provider, snapshots, "sample.csv");
        MarketDataImportService.ImportSummary summary = service.importConfiguredFile();

        assertThat(summary.importedRows()).isEqualTo(1);
        assertThat(summary.duplicateRows()).isEqualTo(1);
        assertThat(summary.rejectedRows()).isZero();
    }

    private MarketDataBatch.MarketDataPoint point(String timestamp) {
        return new MarketDataBatch.MarketDataPoint("AAPL", Instant.parse(timestamp),
                new java.math.BigDecimal("229.45"), 100L, new java.math.BigDecimal("0.021"),
                new java.math.BigDecimal("0.84"), "HISTORICAL_CSV");
    }
}