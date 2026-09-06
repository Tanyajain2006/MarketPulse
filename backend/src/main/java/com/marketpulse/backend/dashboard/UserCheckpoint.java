package com.marketpulse.backend.dashboard;

import java.time.Instant;

import com.marketpulse.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "checkpoints")
public class UserCheckpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserCheckpoint() { }

    public UserCheckpoint(User user, Instant observedAt) { this.user = user; this.observedAt = observedAt; }

    @PrePersist
    void onCreate() { updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Instant getObservedAt() { return observedAt; }
    public void observe(Instant timestamp) { observedAt = timestamp; }
}
