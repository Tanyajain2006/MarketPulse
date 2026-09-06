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

import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;
import com.marketpulse.backend.watchlist.WatchlistDtos.AddWatchlistItemRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.CreateWatchlistRequest;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {
    @Mock private WatchlistRepository watchlists;
    @Mock private WatchlistItemRepository items;
    @Mock private UserRepository users;
    @Mock private InstrumentRepository instruments;
    @Mock private MarketSnapshotRepository snapshots;
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
}