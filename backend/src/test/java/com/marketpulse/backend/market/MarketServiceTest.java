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
}
