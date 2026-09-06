package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class MarketChangeServiceTest {
    @Mock private MarketSnapshotRepository snapshots;
    @Mock private InstrumentRepository instruments;

    @Test
    void derivesAnalysisFromLatestAndEarlierDatabaseSnapshots() {
        MarketSnapshot previous = snapshot("2026-09-01T09:30:00Z", "100", 100L);
        MarketSnapshot current = snapshot("2026-09-01T10:00:00Z", "110", 300L);
        when(snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc("TEST", "VALID"))
                .thenReturn(Optional.of(current));
        when(snapshots.findByTickerAndDataQualityAndObservationTimestampBeforeOrderByObservationTimestampDesc(
                "TEST", "VALID", current.getObservationTimestamp(), PageRequest.of(0, 20)))
                .thenReturn(List.of(previous));

        MarketChangeDtos.ChangeResponse result = service().changes("test");

        assertThat(result.ticker()).isEqualTo("TEST");
        assertThat(result.price().percentageChange()).isEqualByComparingTo("10.0000000000");
        assertThat(result.volume().multiple()).isEqualByComparingTo("3.0000000000");
    }

    @Test
    void returnsNotFoundWhenTickerHasNoValidObservation() {
        when(snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc("MISSING", "VALID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().changes("missing"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    private MarketChangeService service() {
        return new MarketChangeService(snapshots, new MarketService(snapshots, instruments),
                new MarketChangeEngine(1, new BigDecimal("2"), new BigDecimal("25")), 20);
    }

    private MarketSnapshot snapshot(String timestamp, String price, long volume) {
        return new MarketSnapshot("TEST", Instant.parse(timestamp), new BigDecimal(price), volume,
                new BigDecimal("1"), new BigDecimal("0.5"), "TEST");
    }
}