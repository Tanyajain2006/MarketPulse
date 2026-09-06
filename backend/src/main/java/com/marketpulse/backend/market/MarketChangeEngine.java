package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.marketpulse.backend.watchlist.MarketSnapshot;

public final class MarketChangeEngine {
    public enum Status { CALCULATED, INSUFFICIENT_HISTORY, MISSING_DATA, INVALID_INPUT, UNAVAILABLE }
    public enum Signal { NORMAL, ANOMALOUS, UNAVAILABLE }

    private final int minimumHistory;
    private final BigDecimal volumeThreshold;
    private final BigDecimal volatilityThreshold;

        public MarketChangeEngine(
            @Value("${marketpulse.change-engine.minimum-history:1}") int minimumHistory,
            @Value("${marketpulse.change-engine.volume-anomaly-threshold:2.0}") BigDecimal volumeThreshold,
            @Value("${marketpulse.change-engine.volatility-anomaly-threshold:25.0}") BigDecimal volatilityThreshold) {
        if (minimumHistory < 1 || volumeThreshold.signum() < 0 || volatilityThreshold.signum() < 0) {
            throw new IllegalArgumentException("Change-engine configuration must be non-negative and history must be positive.");
        }
        this.minimumHistory = minimumHistory;
        this.volumeThreshold = volumeThreshold;
        this.volatilityThreshold = volatilityThreshold;
    }

    public MarketChangeResult analyze(MarketSnapshot current, MarketSnapshot previous, List<MarketSnapshot> priorObservations) {
        if (current == null) return MarketChangeResult.unavailable(Status.MISSING_DATA);
        PriceResult price = price(current.getPrice(), previous == null ? null : previous.getPrice());
        VolumeResult volume = volume(current.getVolume(), priorObservations);
        VolatilityResult volatility = volatility(current.getVolatility(), previous == null ? null : previous.getVolatility());
        RelativeMovementResult relative = relative(price, current.getSectorChange());
        return new MarketChangeResult(price, volume, volatility, relative,
                new PeerComparisonResult(Status.UNAVAILABLE, "Peer or market data is not available."));
    }

    private PriceResult price(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return new PriceResult(previous, current, null, null, Status.MISSING_DATA, "Both prices are required.");
        if (invalid(current) || invalid(previous) || current.signum() < 0 || previous.signum() < 0) {
            return new PriceResult(previous, current, null, null, Status.INVALID_INPUT, "Prices must be finite and non-negative.");
        }
        BigDecimal absolute = current.subtract(previous);
        if (previous.signum() == 0) return new PriceResult(previous, current, absolute, null, Status.UNAVAILABLE, "Percentage change is unavailable because previous price is zero.");
        return new PriceResult(previous, current, absolute, absolute.divide(previous, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)), Status.CALCULATED, null);
    }

    private VolumeResult volume(Long current, List<MarketSnapshot> prior) {
        if (current == null) return new VolumeResult(null, null, null, volumeThreshold, Signal.UNAVAILABLE, Status.MISSING_DATA, "Current volume is missing.");
        if (current < 0) return new VolumeResult(current, null, null, volumeThreshold, Signal.UNAVAILABLE, Status.INVALID_INPUT, "Volume must be non-negative.");
        List<Long> valid = prior == null ? List.of() : prior.stream().map(snapshot -> snapshot.getVolume()).filter(value -> value != null && value >= 0).toList();
        if (valid.size() < minimumHistory) return new VolumeResult(current, null, null, volumeThreshold, Signal.UNAVAILABLE, Status.INSUFFICIENT_HISTORY, "Not enough prior valid observations for a baseline.");
        BigDecimal baseline = BigDecimal.valueOf(valid.stream().mapToLong(value -> value).sum()).divide(BigDecimal.valueOf(valid.size()), 10, RoundingMode.HALF_UP);
        if (baseline.signum() == 0) return new VolumeResult(current, baseline, null, volumeThreshold, Signal.UNAVAILABLE, Status.UNAVAILABLE, "Volume multiple is unavailable because baseline volume is zero.");
        BigDecimal multiple = BigDecimal.valueOf(current).divide(baseline, 10, RoundingMode.HALF_UP);
        return new VolumeResult(current, baseline, multiple, volumeThreshold, multiple.compareTo(volumeThreshold) >= 0 ? Signal.ANOMALOUS : Signal.NORMAL, Status.CALCULATED, null);
    }

    private VolatilityResult volatility(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return new VolatilityResult(previous, current, null, volatilityThreshold, Signal.UNAVAILABLE, Status.MISSING_DATA, "Both volatility values are required.");
        if (invalid(current) || invalid(previous) || current.signum() < 0 || previous.signum() < 0) return new VolatilityResult(previous, current, null, volatilityThreshold, Signal.UNAVAILABLE, Status.INVALID_INPUT, "Volatility must be finite and non-negative.");
        if (previous.signum() == 0) return new VolatilityResult(previous, current, null, volatilityThreshold, Signal.UNAVAILABLE, Status.UNAVAILABLE, "Relative volatility change is unavailable because previous volatility is zero.");
        BigDecimal change = current.subtract(previous).divide(previous, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return new VolatilityResult(previous, current, change, volatilityThreshold, change.abs().compareTo(volatilityThreshold) >= 0 ? Signal.ANOMALOUS : Signal.NORMAL, Status.CALCULATED, null);
    }

    private RelativeMovementResult relative(PriceResult price, BigDecimal sectorMovement) {
        if (price.percentageChange() == null || sectorMovement == null) return new RelativeMovementResult(null, sectorMovement, null, Status.MISSING_DATA, "Stock or sector movement is missing.");
        if (invalid(sectorMovement)) return new RelativeMovementResult(null, sectorMovement, null, Status.INVALID_INPUT, "Sector movement must be finite.");
        BigDecimal relative = price.percentageChange().subtract(sectorMovement);
        String interpretation = relative.signum() > 0 ? "OUTPERFORMING_SECTOR" : relative.signum() < 0 ? "UNDERPERFORMING_SECTOR" : "MATCHING_SECTOR";
        return new RelativeMovementResult(price.percentageChange(), sectorMovement, relative, Status.CALCULATED, interpretation);
    }

    private boolean invalid(BigDecimal value) { return value == null; }

    public record PriceResult(BigDecimal previousPrice, BigDecimal currentPrice, BigDecimal absoluteChange,
            BigDecimal percentageChange, Status status, String reason) { }
    public record VolumeResult(Long currentVolume, BigDecimal historicalBaseline, BigDecimal multiple,
            BigDecimal threshold, Signal signal, Status status, String reason) { }
    public record VolatilityResult(BigDecimal previousVolatility, BigDecimal currentVolatility, BigDecimal relativeChange,
            BigDecimal threshold, Signal signal, Status status, String reason) { }
    public record RelativeMovementResult(BigDecimal stockMovement, BigDecimal sectorMovement, BigDecimal relativeMovement,
            Status status, String interpretation) { }
    public record PeerComparisonResult(Status status, String reason) { }
    public record MarketChangeResult(PriceResult price, VolumeResult volume, VolatilityResult volatility,
            RelativeMovementResult relativeMovement, PeerComparisonResult peerComparison) {
        static MarketChangeResult unavailable(Status status) {
            return new MarketChangeResult(new PriceResult(null, null, null, null, status, "Current observation is unavailable."),
                    new VolumeResult(null, null, null, null, Signal.UNAVAILABLE, status, "Current observation is unavailable."),
                    new VolatilityResult(null, null, null, null, Signal.UNAVAILABLE, status, "Current observation is unavailable."),
                    new RelativeMovementResult(null, null, null, status, "UNAVAILABLE"),
                    new PeerComparisonResult(Status.UNAVAILABLE, "Peer or market data is not available."));
        }
    }
}