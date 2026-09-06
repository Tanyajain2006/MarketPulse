package com.marketpulse.backend.watchlist;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketpulse.backend.dashboard.UserCheckpointRepository;
import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;
import com.marketpulse.backend.watchlist.WatchlistDtos.AddWatchlistItemRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.CreateWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.WatchlistResponse;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {
    @Mock private WatchlistRepository watchlists;
    @Mock private WatchlistItemRepository items;
    @Mock private UserRepository users;
    @Mock private InstrumentRepository instruments;
    @Mock private MarketSnapshotRepository snapshots;
    @Mock private UserCheckpointRepository checkpoints;
    private WatchlistService service;

    @BeforeEach
    void setUp() { service = new WatchlistService(watchlists, items, users, instruments, snapshots); }

    @Test
    void createsWatchlistForAuthenticatedUser() {
        User user = new User("Ada", "ada@example.com", "hash");
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(watchlists.save(any(Watchlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WatchlistDtos.WatchlistResponse response = service.create("ada@example.com", new CreateWatchlistRequest("  Growth  "));

        assertThat(response.name()).isEqualTo("Growth");
        verify(users).findByEmail("ada@example.com");
    }

    @Test
    void treatsAnotherUsersWatchlistAsNotFound() {
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("ada@example.com", 7L))
                .isInstanceOf(WatchlistNotFoundException.class);
    }

    @Test
    void rejectsDuplicateTickerWithinWatchlist() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(items.existsByWatchlistIdAndTicker(7L, "AAPL")).thenReturn(true);

        assertThatThrownBy(() -> service.addItem("ada@example.com", 7L, new AddWatchlistItemRequest("aapl")))
                .isInstanceOf(DuplicateTickerException.class);
        verify(items, never()).save(any(WatchlistItem.class));
    }

    @Test
    void normalizesAndPersistsTicker() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(instruments.findByTickerIgnoreCase("AAPL"))
            .thenReturn(Optional.of(new Instrument("AAPL", "Apple Inc.", "NASDAQ")));
        when(items.saveAndFlush(any(WatchlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WatchlistResponse response = service.addItem("ada@example.com", 7L, new AddWatchlistItemRequest(" aapl "));

        assertThat(response.items()).extracting(WatchlistDtos.WatchlistItemResponse::ticker).containsExactly("AAPL");
    }

    @Test
    void removesNormalizedTickerFromOwnedWatchlist() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        watchlist.getItems().add(new WatchlistItem(watchlist, "AAPL"));
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));

        WatchlistResponse response = service.removeItem("ada@example.com", 7L, " aapl ");

        verify(items).deleteByWatchlistIdAndTicker(7L, "AAPL");
        assertThat(response.items()).isEmpty();
    }

    @Test
    void deletesCheckpointsWhenDeletingOwnedWatchlist() {
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(
                new Watchlist(new User("Ada", "ada@example.com", "hash"), "Growth")));
        WatchlistService value = new WatchlistService(watchlists, items, users, instruments, snapshots, checkpoints);

        value.delete("ada@example.com", 7L);

        verify(checkpoints).deleteByWatchlistId(7L);
        verify(watchlists).deleteById(7L);
    }
}