package com.marketpulse.backend.watchlist;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketpulse.backend.dashboard.UserCheckpoint;
import com.marketpulse.backend.dashboard.UserCheckpointRepository;
import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class CheckpointServiceTest {
    @Mock private UserRepository users;
    @Mock private WatchlistRepository watchlists;
    @Mock private UserCheckpointRepository checkpoints;

    @Test
    void createsServerTimestampForOwnedWatchlist() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(checkpoints.findByUserEmailAndWatchlistId("ada@example.com", 7L)).thenReturn(Optional.empty());
        when(checkpoints.save(any(UserCheckpoint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        CheckpointDtos.CheckpointResponse response = new CheckpointService(users, watchlists, checkpoints)
                .review("ada@example.com", 7L);

        assertThat(response.watchlistId()).isEqualTo(7L);
        assertThat(response.reviewedAt()).isBetween(before, Instant.now());
    }

    @Test
    void upsertsExistingCheckpointWithoutCreatingAnotherRow() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        UserCheckpoint existing = new UserCheckpoint(user, watchlist, Instant.parse("2026-09-01T10:00:00Z"));
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(checkpoints.findByUserEmailAndWatchlistId("ada@example.com", 7L)).thenReturn(Optional.of(existing));
        when(checkpoints.save(existing)).thenReturn(existing);

        CheckpointDtos.CheckpointResponse response = new CheckpointService(users, watchlists, checkpoints)
                .review("ada@example.com", 7L);

        assertThat(response.reviewedAt()).isAfter(Instant.parse("2026-09-01T10:00:00Z"));
        verify(checkpoints).save(existing);
    }

    @Test
    void getDoesNotCreateMissingCheckpoint() {
        User user = new User("Ada", "ada@example.com", "hash");
        Watchlist watchlist = new Watchlist(user, "Growth");
        when(watchlists.findByIdAndUserEmail(7L, "ada@example.com")).thenReturn(Optional.of(watchlist));
        when(checkpoints.findByUserEmailAndWatchlistId("ada@example.com", 7L)).thenReturn(Optional.empty());

        assertThat(new CheckpointService(users, watchlists, checkpoints).get("ada@example.com", 7L)).isNull();
        verify(checkpoints, never()).save(any());
    }

    @Test
    void rejectsWatchlistOwnedByAnotherUser() {
        when(watchlists.findByIdAndUserEmail(7L, "other@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CheckpointService(users, watchlists, checkpoints).review("other@example.com", 7L))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        verify(checkpoints, never()).save(any());
    }
}