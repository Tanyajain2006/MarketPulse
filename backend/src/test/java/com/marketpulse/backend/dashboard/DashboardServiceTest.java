package com.marketpulse.backend.dashboard;

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
import org.springframework.data.domain.Pageable;

import com.marketpulse.backend.market.NewsEventRepository;
import com.marketpulse.backend.user.UserRepository;
import com.marketpulse.backend.watchlist.Instrument;
import com.marketpulse.backend.watchlist.InstrumentRepository;
import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;
import com.marketpulse.backend.watchlist.Watchlist;
import com.marketpulse.backend.watchlist.WatchlistRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock private UserRepository users;
    @Mock private WatchlistRepository watchlists;
    @Mock private UserCheckpointRepository checkpoints;
    @Mock private MarketSnapshotRepository snapshots;
    @Mock private NewsEventRepository news;
    @Mock private InstrumentRepository instruments;

    @Test
    void calculatesPriceChangeAndDeterministicClassification() {
        var user = new com.marketpulse.backend.user.User("Ada", "ada@example.com", "hash");
        var watchlist = new Watchlist(user, "Growth");
        watchlist.getItems().add(new com.marketpulse.backend.watchlist.WatchlistItem(watchlist, "AAPL"));
        Instant previousTime = Instant.parse("2026-08-31T09:30:00Z");
        Instant currentTime = Instant.parse("2026-08-31T09:35:00Z");
        var previous = new MarketSnapshot("AAPL", previousTime, new BigDecimal("100"), 100L, new BigDecimal("1"), new BigDecimal("0"), "TEST");
        var current = new MarketSnapshot("AAPL", currentTime, new BigDecimal("120"), 300L, new BigDecimal("2"), new BigDecimal("1"), "TEST");
        when(watchlists.findAllByUserEmailOrderByUpdatedAtDesc("ada@example.com")).thenReturn(List.of(watchlist));
        when(checkpoints.findByUserEmailAndWatchlistId("ada@example.com", watchlist.getId())).thenReturn(Optional.empty());
        when(snapshots.findSinceForTicker("AAPL", Instant.EPOCH, Pageable.ofSize(25))).thenReturn(List.of(current, previous));
        when(instruments.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(new Instrument("AAPL", "Apple Inc.", "NASDAQ")));
        when(news.findFiltered("AAPL", Instant.EPOCH, currentTime, null, Pageable.ofSize(1))).thenReturn(List.of());

        var service = new DashboardService(users, watchlists, checkpoints, snapshots, news, instruments);
        var result = service.overview("ada@example.com");

        assertThat(result.changes()).singleElement().satisfies(change -> {
            assertThat(change.priceChangePercent()).isEqualByComparingTo("20.000000");
            assertThat(change.volumeMultiple()).isEqualByComparingTo("3.00");
            assertThat(change.classification()).isEqualTo("MEANINGFUL");
        });
    }
}
