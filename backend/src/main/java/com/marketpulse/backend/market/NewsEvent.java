package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "news_events", indexes = {
        @Index(name = "idx_news_ticker_timestamp", columnList = "ticker,timestamp"),
        @Index(name = "idx_news_timestamp", columnList = "timestamp")
}, uniqueConstraints = @UniqueConstraint(name = "uk_news_event_identity", columnNames = { "ticker", "timestamp", "headline" }))
public class NewsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String ticker;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 500)
    private String headline;

    @Column(nullable = false, length = 120)
    private String source;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "sentiment_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal sentimentScore;

    protected NewsEvent() { }

    public NewsEvent(String ticker, Instant timestamp, String headline, String source, String eventType,
            BigDecimal sentimentScore) {
        this.ticker = ticker;
        this.timestamp = timestamp;
        this.headline = headline;
        this.source = source;
        this.eventType = eventType;
        this.sentimentScore = sentimentScore;
    }

    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public Instant getTimestamp() { return timestamp; }
    public String getHeadline() { return headline; }
    public String getSource() { return source; }
    public String getEventType() { return eventType; }
    public BigDecimal getSentimentScore() { return sentimentScore; }
}
