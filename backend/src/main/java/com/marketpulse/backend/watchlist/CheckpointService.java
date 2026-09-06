package com.marketpulse.backend.watchlist;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.dashboard.UserCheckpoint;
import com.marketpulse.backend.dashboard.UserCheckpointRepository;
import com.marketpulse.backend.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CheckpointService {
    private final UserRepository users;
    private final WatchlistRepository watchlists;
    private final UserCheckpointRepository checkpoints;

    public CheckpointService(UserRepository users, WatchlistRepository watchlists, UserCheckpointRepository checkpoints) {
        this.users = users;
        this.watchlists = watchlists;
        this.checkpoints = checkpoints;
    }

    @Transactional
    public CheckpointDtos.CheckpointResponse review(String email, Long watchlistId) {
        Watchlist watchlist = ownedWatchlist(email, watchlistId);
        var user = users.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        Instant reviewedAt = Instant.now();
        UserCheckpoint checkpoint = checkpoints.findByUserEmailAndWatchlistId(email, watchlistId)
                .map(existing -> { existing.review(reviewedAt); return existing; })
                .orElseGet(() -> new UserCheckpoint(user, watchlist, reviewedAt));
        UserCheckpoint saved = checkpoints.save(Objects.requireNonNull(checkpoint));
        return response(saved, watchlistId);
    }

    @Transactional(readOnly = true)
    public CheckpointDtos.CheckpointResponse get(String email, Long watchlistId) {
        ownedWatchlist(email, watchlistId);
        return checkpoints.findByUserEmailAndWatchlistId(email, watchlistId)
            .map(checkpoint -> response(checkpoint, watchlistId)).orElse(null);
    }

    private Watchlist ownedWatchlist(String email, Long watchlistId) {
        if (watchlistId == null || watchlistId <= 0) throw new IllegalArgumentException("Watchlist id must be positive.");
        return watchlists.findByIdAndUserEmail(watchlistId, email).orElseThrow(EntityNotFoundException::new);
    }

    private CheckpointDtos.CheckpointResponse response(UserCheckpoint checkpoint, Long watchlistId) {
        return new CheckpointDtos.CheckpointResponse(checkpoint.getId(), watchlistId, checkpoint.getReviewedAt());
    }
}