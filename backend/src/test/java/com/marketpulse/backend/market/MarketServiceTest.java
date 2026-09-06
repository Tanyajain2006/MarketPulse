package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.marketpulse.backend.watchlist.InstrumentRepository;
import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {
    @Mock private MarketSnapshotRepository snapshots;
    @Mock private InstrumentRepository instruments;

    @Test
    void latestNormalizesAndDeduplicatesTickerQuery() {
        MarketSnapshot value = new MarketSnapshot("AAPL", Instant.parse("2026-08-31T10:00:00Z"),
                new BigDecimal("220.10"), 100L, new BigDecimal("1.0"), new BigDecimal("0.2"), "TEST");
        when(snapshots.findLatestForTickers(List.of("AAPL", "NVDA"))).thenReturn(List.of(value));
        MarketService service = new MarketService(snapshots, instruments);

        List<MarketSnapshotDtos.SnapshotResponse> result = service.latest(List.of(" aapl ", "NVDA", "AAPL"));

        assertThat(result).extracting(MarketSnapshotDtos.SnapshotResponse::ticker).containsExactly("AAPL");
    }

    @Test
    void queriesTickerHistoryWithinObservationRange() {
        Instant from = Instant.parse("2026-09-01T09:00:00Z");
        Instant to = Instant.parse("2026-09-01T11:00:00Z");
        when(snapshots.findByTickerAndDataQualityAndObservationTimestampBetweenOrderByObservationTimestampAsc(
            "AAPL", "VALID", from, to, PageRequest.of(0, 100))).thenReturn(List.of());
        MarketService service = new MarketService(snapshots, instruments);

        assertThat(service.history("aapl", from, to, 100)).isEmpty();
    }

    @Test
    void rejectsReversedObservationRange() {
        MarketService service = new MarketService(snapshots, instruments);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.history("AAPL",
                Instant.parse("2026-09-02T00:00:00Z"), Instant.parse("2026-09-01T00:00:00Z"), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be before");
    }

    @Test
    void mapsObservationAndIngestionTimestampsSeparately() {
        Instant observation = Instant.parse("2026-09-01T09:30:00Z");
        Instant ingestion = Instant.parse("2026-09-06T10:00:00Z");
        MarketSnapshot value = new MarketSnapshot("AAPL", observation, new BigDecimal("220.10"), 100L,
                new BigDecimal("1.0"), new BigDecimal("0.2"), "HISTORICAL_CSV", "VALID", ingestion);
        when(snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc("AAPL", "VALID"))
                .thenReturn(java.util.Optional.of(value));

        MarketSnapshotDtos.SnapshotResponse result = new MarketService(snapshots, instruments).latest("aapl");

        assertThat(result.observationTimestamp()).isEqualTo(observation);
        assertThat(result.ingestionTimestamp()).isEqualTo(ingestion);
    }
}
