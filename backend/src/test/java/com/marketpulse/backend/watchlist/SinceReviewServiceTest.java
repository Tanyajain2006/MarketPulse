package com.marketpulse.backend.watchlist;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.marketpulse.backend.dashboard.UserCheckpoint;
import com.marketpulse.backend.dashboard.UserCheckpointRepository;
import com.marketpulse.backend.market.MarketChangeEngine;
import com.marketpulse.backend.market.MaterialityScoreEngine;
import com.marketpulse.backend.user.User;

@ExtendWith(MockitoExtension.class)
class SinceReviewServiceTest {
    @Mock private WatchlistRepository watchlists;
    @Mock private UserCheckpointRepository checkpoints;
    @Mock private MarketSnapshotRepository snapshots;

    @Test
    void returnsNoCheckpointWithoutReadingMarketSnapshots() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(checkpoints.findByUserEmailAndWatchlistId("ada@example.com", 7L)).thenReturn(Optional.empty());

        SinceReviewDtos.SinceReviewResponse response = service().sinceLastReview("ada@example.com", 7L);

        assertThat(response.status()).isEqualTo("NO_CHECKPOINT");
        assertThat(response.stocks()).isEmpty();
    }

    @Test
    void returnsCalculatedStockWithScoreAndContributions() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        watchlist.getItems().add(new WatchlistItem(watchlist, "TEST"));
        Instant reviewed = Instant.parse("2026-09-01T09:45:00Z");
        UserCheckpoint checkpoint = new UserCheckpoint(user, watchlist, reviewed);
        MarketSnapshot previous = snapshot("2026-09-01T09:30:00Z", "100", 100L);
        MarketSnapshot current = snapshot("2026-09-01T10:00:00Z", "110", 300L);
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(checkpoints.findByUserEmailAndWatchlistId("ada@example.com", 7L)).thenReturn(Optional.of(checkpoint));
        when(snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc("TEST", "VALID")).thenReturn(Optional.of(current));
        when(snapshots.findFirstByTickerAndDataQualityAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc("TEST", "VALID", reviewed)).thenReturn(Optional.of(previous));
        when(snapshots.findByTickerAndDataQualityAndObservationTimestampBeforeOrderByObservationTimestampDesc("TEST", "VALID", current.getObservationTimestamp(), PageRequest.of(0, 20))).thenReturn(List.of(previous));

        SinceReviewDtos.StockChange stock = service().sinceLastReview("ada@example.com", 7L).stocks().get(0);

        assertThat(stock.status()).isEqualTo(MarketChangeEngine.Status.CALCULATED);
        assertThat(stock.materialityScore()).isNotNull();
        assertThat(stock.contributions()).hasSize(5);
        assertThat(stock.previousObservationTimestamp()).isEqualTo(previous.getObservationTimestamp());
    }

    private SinceReviewService service() {
        return new SinceReviewService(watchlists, checkpoints, snapshots, new MarketChangeEngine(1, new BigDecimal("2"), new BigDecimal("25")), new MaterialityScoreEngine(), 20, 24);
    }

    private MarketSnapshot snapshot(String timestamp, String price, long volume) {
        return new MarketSnapshot("TEST", Instant.parse(timestamp), new BigDecimal(price), volume,
                new BigDecimal("1"), new BigDecimal("0.5"), "TEST");
    }
}