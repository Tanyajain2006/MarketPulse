package com.marketpulse.backend.watchlist;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "watchlist_items", uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_ticker", columnNames = {"watchlist_id", "ticker"}))
public class WatchlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    @Column(nullable = false, length = 15)
    private String ticker;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(length = 30)
    private String exchange;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WatchlistItem() { }

    public WatchlistItem(Watchlist watchlist, String ticker) {
        this.watchlist = watchlist;
        this.ticker = ticker;
    }

    public WatchlistItem(Watchlist watchlist, Instrument instrument) {
        this.watchlist = watchlist;
        this.ticker = instrument.getTicker();
        this.companyName = instrument.getCompanyName();
        this.exchange = instrument.getExchange();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Watchlist getWatchlist() { return watchlist; }
    public String getTicker() { return ticker; }
    public String getCompanyName() { return companyName; }
    public String getExchange() { return exchange; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}