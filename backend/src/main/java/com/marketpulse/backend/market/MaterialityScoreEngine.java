package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class MaterialityScoreEngine {
    public enum Classification { NORMAL, WORTH_WATCHING, MEANINGFUL, CRITICAL }

    private final BigDecimal priceThreshold;
    private final BigDecimal sectorThreshold;
    private final BigDecimal priceWeight;
    private final BigDecimal volumeWeight;
    private final BigDecimal volatilityWeight;
    private final BigDecimal sectorWeight;
    private final BigDecimal peerWeight;

    public MaterialityScoreEngine(BigDecimal priceThreshold, BigDecimal sectorThreshold,
            BigDecimal priceWeight, BigDecimal volumeWeight, BigDecimal volatilityWeight,
            BigDecimal sectorWeight, BigDecimal peerWeight) {
        this.priceThreshold = nonNegative(priceThreshold);
        this.sectorThreshold = nonNegative(sectorThreshold);
        this.priceWeight = nonNegative(priceWeight);
        this.volumeWeight = nonNegative(volumeWeight);
        this.volatilityWeight = nonNegative(volatilityWeight);
        this.sectorWeight = nonNegative(sectorWeight);
        this.peerWeight = nonNegative(peerWeight);
        if (this.priceWeight.add(this.volumeWeight).add(this.volatilityWeight)
                .add(this.sectorWeight).add(this.peerWeight).compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("Materiality weights must total 100.");
        }
    }

    public MaterialityScoreEngine() {
        this(new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("30"),
                new BigDecimal("25"), new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"));
    }

    public MaterialityResult score(MarketChangeEngine.MarketChangeResult change) {
        List<ComponentContribution> contributions = new ArrayList<>();
        contributions.add(contribution("price anomaly", normalized(change.price().percentageChange(), priceThreshold), priceWeight));
        contributions.add(contribution("volume anomaly", signalValue(change.volume().signal()), volumeWeight));
        contributions.add(contribution("volatility anomaly", signalValue(change.volatility().signal()), volatilityWeight));
        contributions.add(contribution("sector divergence", normalized(change.relativeMovement().relativeMovement(), sectorThreshold), sectorWeight));
        contributions.add(new ComponentContribution("peer divergence", BigDecimal.ZERO, peerWeight, BigDecimal.ZERO, "UNAVAILABLE"));
        BigDecimal total = contributions.stream().map(item -> item.contribution()).reduce(BigDecimal.ZERO, (left, right) -> left.add(right))
                .min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return new MaterialityResult(total, classify(total), List.copyOf(contributions));
    }

    private ComponentContribution contribution(String name, BigDecimal intensity, BigDecimal weight) {
        BigDecimal contribution = intensity.multiply(weight).min(weight).setScale(2, RoundingMode.HALF_UP);
        return new ComponentContribution(name, intensity, weight, contribution, intensity.signum() == 0 ? "NORMAL" : "ANOMALOUS");
    }

    private BigDecimal normalized(BigDecimal value, BigDecimal threshold) {
        if (value == null || threshold.signum() == 0) return BigDecimal.ZERO;
        return value.abs().divide(threshold, 10, RoundingMode.HALF_UP).min(BigDecimal.ONE);
    }

    private BigDecimal signalValue(MarketChangeEngine.Signal signal) {
        return signal == MarketChangeEngine.Signal.ANOMALOUS ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Materiality configuration must be non-negative.");
        return value;
    }

    private Classification classify(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(81)) >= 0) return Classification.CRITICAL;
        if (score.compareTo(BigDecimal.valueOf(61)) >= 0) return Classification.MEANINGFUL;
        if (score.compareTo(BigDecimal.valueOf(31)) >= 0) return Classification.WORTH_WATCHING;
        return Classification.NORMAL;
    }

    public record ComponentContribution(String name, BigDecimal intensity, BigDecimal weight,
            BigDecimal contribution, String status) { }
    public record MaterialityResult(BigDecimal score, Classification classification,
            List<ComponentContribution> contributions) { }
}