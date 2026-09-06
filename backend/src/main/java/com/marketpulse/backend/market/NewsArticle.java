package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "news_articles", indexes = {
        @Index(name = "idx_news_article_ticker_published", columnList = "ticker,published_at"),
        @Index(name = "idx_news_article_published", columnList = "published_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_news_article_identity",
        columnNames = { "ticker", "published_at", "headline", "source" }))
public class NewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String ticker;

    @Column(nullable = false, length = 500)
    private String headline;

    @Column(nullable = false, length = 120)
    private String source;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "ingestion_timestamp", nullable = false)
    private Instant ingestionTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private NewsEventType eventType;

    @Column(name = "sentiment_score", precision = 8, scale = 4)
    private BigDecimal sentimentScore;

    protected NewsArticle() { }

    public NewsArticle(String ticker, String headline, String source, Instant publishedAt,
            Instant ingestionTimestamp, NewsEventType eventType) {
        this.ticker = ticker;
        this.headline = headline;
        this.source = source;
        this.publishedAt = publishedAt;
        this.ingestionTimestamp = ingestionTimestamp;
        this.eventType = eventType;
    }

    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public String getHeadline() { return headline; }
    public String getSource() { return source; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getIngestionTimestamp() { return ingestionTimestamp; }
    public NewsEventType getEventType() { return eventType; }
    public BigDecimal getSentimentScore() { return sentimentScore; }
}