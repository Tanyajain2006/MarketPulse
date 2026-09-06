package com.marketpulse.backend.watchlist;

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
@Table(name = "market_snapshots", indexes = {
    @Index(name = "idx_snapshot_ticker", columnList = "ticker"),
    @Index(name = "idx_snapshot_observation_timestamp", columnList = "observation_timestamp"),
    @Index(name = "idx_snapshot_ticker_observation_timestamp", columnList = "ticker,observation_timestamp")
}, uniqueConstraints = @UniqueConstraint(name = "uk_snapshot_identity", columnNames = { "ticker", "observation_timestamp" }))
public class MarketSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String ticker;

    @Column(name = "observation_timestamp", nullable = false)
    private Instant observationTimestamp;

    @Column(name = "ingestion_timestamp", nullable = false)
    private Instant ingestionTimestamp;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal volatility;

    @Column(name = "sector_change", nullable = false, precision = 10, scale = 4)
    private BigDecimal sectorChange;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "data_quality", nullable = false, length = 30)
    private String dataQuality;

    protected MarketSnapshot() { }

    public MarketSnapshot(String ticker, Instant timestamp, BigDecimal price, Long volume,
            BigDecimal volatility, BigDecimal sectorChange, String source) {
        this(ticker, timestamp, price, volume, volatility, sectorChange, source, "VALID", Instant.now());
    }

    public MarketSnapshot(String ticker, Instant observationTimestamp, BigDecimal price, Long volume,
            BigDecimal volatility, BigDecimal sectorChange, String source, String dataQuality,
            Instant ingestionTimestamp) {
        this.ticker = ticker;
        this.observationTimestamp = observationTimestamp;
        this.price = price;
        this.volume = volume;
        this.volatility = volatility;
        this.sectorChange = sectorChange;
        this.source = source;
        this.dataQuality = dataQuality;
        this.ingestionTimestamp = ingestionTimestamp;
    }

    public String getTicker() { return ticker; }
    public Instant getObservationTimestamp() { return observationTimestamp; }
    public Instant getTimestamp() { return observationTimestamp; }
    public Instant getIngestionTimestamp() { return ingestionTimestamp; }
    public BigDecimal getPrice() { return price; }
    public Long getVolume() { return volume; }
    public BigDecimal getVolatility() { return volatility; }
    public BigDecimal getSectorChange() { return sectorChange; }
    public String getSource() { return source; }
    public String getDataQuality() { return dataQuality; }
}