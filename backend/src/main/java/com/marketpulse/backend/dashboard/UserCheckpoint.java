package com.marketpulse.backend.dashboard;

import java.time.Instant;

import com.marketpulse.backend.user.User;
import com.marketpulse.backend.watchlist.Watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_checkpoints", indexes = {
    @Index(name = "idx_checkpoint_user", columnList = "user_id"),
    @Index(name = "idx_checkpoint_watchlist", columnList = "watchlist_id"),
    @Index(name = "idx_checkpoint_user_watchlist", columnList = "user_id,watchlist_id")
}, uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uk_checkpoint_user_watchlist", columnNames = { "user_id", "watchlist_id" }))
public class UserCheckpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected UserCheckpoint() { }

    public UserCheckpoint(User user, Watchlist watchlist, Instant reviewedAt) {
        this.user = user;
        this.watchlist = watchlist;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Watchlist getWatchlist() { return watchlist; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void review(Instant timestamp) { reviewedAt = timestamp; }
}
