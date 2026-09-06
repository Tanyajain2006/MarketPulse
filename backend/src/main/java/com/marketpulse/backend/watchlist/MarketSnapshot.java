package com.marketpulse.backend.watchlist;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "market_snapshots")
public class MarketSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String ticker;

    @Column(nullable = false)
    private Instant timestamp;

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

    protected MarketSnapshot() { }

    public MarketSnapshot(String ticker, Instant timestamp, BigDecimal price, Long volume,
            BigDecimal volatility, BigDecimal sectorChange, String source) {
        this.ticker = ticker;
        this.timestamp = timestamp;
        this.price = price;
        this.volume = volume;
        this.volatility = volatility;
        this.sectorChange = sectorChange;
        this.source = source;
    }

    public String getTicker() { return ticker; }
    public Instant getTimestamp() { return timestamp; }
    public BigDecimal getPrice() { return price; }
    public Long getVolume() { return volume; }
    public BigDecimal getVolatility() { return volatility; }
    public BigDecimal getSectorChange() { return sectorChange; }
    public String getSource() { return source; }
}