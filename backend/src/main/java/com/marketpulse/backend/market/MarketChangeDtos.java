package com.marketpulse.backend.market;

import java.math.BigDecimal;

public final class MarketChangeDtos {
    private MarketChangeDtos() { }

    public record ChangeResponse(String ticker, String observationTimestamp, Price price, Volume volume,
            Volatility volatility, RelativeMovement relativeMovement, PeerComparison peerComparison) { }
    public record Price(BigDecimal previousPrice, BigDecimal currentPrice, BigDecimal absoluteChange,
            BigDecimal percentageChange, MarketChangeEngine.Status status, String reason) { }
    public record Volume(Long currentVolume, BigDecimal historicalBaseline, BigDecimal multiple,
            BigDecimal threshold, MarketChangeEngine.Signal signal, MarketChangeEngine.Status status, String reason) { }
    public record Volatility(BigDecimal previousVolatility, BigDecimal currentVolatility, BigDecimal relativeChange,
            BigDecimal threshold, MarketChangeEngine.Signal signal, MarketChangeEngine.Status status, String reason) { }
    public record RelativeMovement(BigDecimal stockMovement, BigDecimal sectorMovement, BigDecimal relativeMovement,
            MarketChangeEngine.Status status, String interpretation) { }
    public record PeerComparison(MarketChangeEngine.Status status, String reason) { }
}