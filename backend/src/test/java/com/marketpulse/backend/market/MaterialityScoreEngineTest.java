package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.marketpulse.backend.watchlist.MarketSnapshot;

class MaterialityScoreEngineTest {
    @Test
    void classifiesConfiguredScoreBoundaries() {
        MaterialityScoreEngine engine = new MaterialityScoreEngine(
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        MarketChangeEngine.MarketChangeResult normal = result("0");
        MarketChangeEngine.MarketChangeResult high = result("1");

        assertThat(engine.score(normal).classification()).isEqualTo(MaterialityScoreEngine.Classification.NORMAL);
        assertThat(engine.score(high).classification()).isEqualTo(MaterialityScoreEngine.Classification.CRITICAL);
    }

    @Test
    void returnsExplainableComponentContributionsAndIsDeterministic() {
        MaterialityScoreEngine engine = new MaterialityScoreEngine();
        MarketChangeEngine.MarketChangeResult result = new MarketChangeEngine(1, new BigDecimal("2"), new BigDecimal("25")).analyze(
                snapshot("110", 300L, "1.5", "2"), snapshot("100", 100L, "1", "0.5"),
                List.of(snapshot("100", 100L, "1", "0.5")));

        var first = engine.score(result);
        var second = engine.score(result);

        assertThat(first).isEqualTo(second);
        assertThat(first.contributions()).extracting(MaterialityScoreEngine.ComponentContribution::name)
                .containsExactly("price anomaly", "volume anomaly", "volatility anomaly", "sector divergence", "peer divergence");
        assertThat(first.score()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    void rejectsWeightsThatDoNotTotalOneHundred() {
        assertThatThrownBy(() -> new MaterialityScoreEngine(BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MarketChangeEngine.MarketChangeResult result(String movement) {
        var price = new MarketChangeEngine.PriceResult(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO,
                new BigDecimal(movement), MarketChangeEngine.Status.CALCULATED, null);
        var volume = new MarketChangeEngine.VolumeResult(1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                MarketChangeEngine.Signal.NORMAL, MarketChangeEngine.Status.CALCULATED, null);
        var volatility = new MarketChangeEngine.VolatilityResult(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ONE, MarketChangeEngine.Signal.NORMAL, MarketChangeEngine.Status.CALCULATED, null);
        var relative = new MarketChangeEngine.RelativeMovementResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                MarketChangeEngine.Status.CALCULATED, "MATCHING_SECTOR");
        return new MarketChangeEngine.MarketChangeResult(price, volume, volatility, relative,
                new MarketChangeEngine.PeerComparisonResult(MarketChangeEngine.Status.UNAVAILABLE, "unavailable"));
    }

    private MarketSnapshot snapshot(String price, long volume, String volatility, String sector) {
        return new MarketSnapshot("TEST", Instant.parse("2026-09-01T10:00:00Z"), new BigDecimal(price), volume,
                new BigDecimal(volatility), new BigDecimal(sector), "TEST");
    }
}