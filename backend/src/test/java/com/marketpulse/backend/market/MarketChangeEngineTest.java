package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.marketpulse.backend.watchlist.MarketSnapshot;

class MarketChangeEngineTest {
    private final MarketChangeEngine engine = new MarketChangeEngine(1, new BigDecimal("2.0"), new BigDecimal("25"));

    @Test
    void calculatesPriceVolumeVolatilityAndRelativeMovementDeterministically() {
        MarketSnapshot previous = snapshot("2026-09-01T09:30:00Z", "100", 100L, "1.0", "0.5");
        MarketSnapshot current = snapshot("2026-09-01T10:00:00Z", "110", 300L, "1.5", "2.0");

        MarketChangeEngine.MarketChangeResult result = engine.analyze(current, previous, List.of(previous));

        assertThat(result.price().absoluteChange()).isEqualByComparingTo("10");
        assertThat(result.price().percentageChange()).isEqualByComparingTo("10.0000000000");
        assertThat(result.volume().historicalBaseline()).isEqualByComparingTo("100");
        assertThat(result.volume().multiple()).isEqualByComparingTo("3.0000000000");
        assertThat(result.volume().signal()).isEqualTo(MarketChangeEngine.Signal.ANOMALOUS);
        assertThat(result.volatility().relativeChange()).isEqualByComparingTo("50.0000000000");
        assertThat(result.volatility().signal()).isEqualTo(MarketChangeEngine.Signal.ANOMALOUS);
        assertThat(result.relativeMovement().relativeMovement()).isEqualByComparingTo("8.0000000000");
        assertThat(result.peerComparison().status()).isEqualTo(MarketChangeEngine.Status.UNAVAILABLE);
    }

    @Test
    void excludesCurrentObservationFromBaselineAndUsesConfiguredWindow() {
        MarketSnapshot previous = snapshot("2026-09-01T09:30:00Z", "100", 100L, "1", "0");
        MarketSnapshot older = snapshot("2026-09-01T09:00:00Z", "100", 200L, "1", "0");
        MarketChangeEngine configured = new MarketChangeEngine(1, new BigDecimal("2"), new BigDecimal("25"));

        var result = configured.analyze(snapshot("2026-09-01T10:00:00Z", "100", 250L, "1", "0"), previous, List.of(previous, older));

        assertThat(result.volume().historicalBaseline()).isEqualByComparingTo("150");
        assertThat(result.volume().multiple()).isEqualByComparingTo("1.6666666667");
        assertThat(result.volume().signal()).isEqualTo(MarketChangeEngine.Signal.NORMAL);
    }

    @Test
    void handlesZeroMissingInvalidAndInsufficientInputsWithoutNan() {
        MarketSnapshot zeroPrevious = snapshot("2026-09-01T09:30:00Z", "0", 0L, "0", "0");
        MarketSnapshot current = snapshot("2026-09-01T10:00:00Z", "10", 10L, "1", "0");
        var zero = engine.analyze(current, zeroPrevious, List.of(zeroPrevious));
        var missing = engine.analyze(current, null, List.of());
        var invalid = engine.analyze(snapshot("2026-09-01T10:00:00Z", "-1", -1L, "-1", "0"), zeroPrevious, List.of(zeroPrevious));

        assertThat(zero.price().percentageChange()).isNull();
        assertThat(zero.price().status()).isEqualTo(MarketChangeEngine.Status.UNAVAILABLE);
        assertThat(missing.price().status()).isEqualTo(MarketChangeEngine.Status.MISSING_DATA);
        assertThat(invalid.price().status()).isEqualTo(MarketChangeEngine.Status.INVALID_INPUT);
        assertThat(invalid.volume().status()).isEqualTo(MarketChangeEngine.Status.INVALID_INPUT);
        assertThat(engine.analyze(current, null, List.of()).volume().status()).isEqualTo(MarketChangeEngine.Status.INSUFFICIENT_HISTORY);
    }

    @Test
    void detectsDecreaseUnchangedAndSectorInterpretations() {
        MarketSnapshot previous = snapshot("2026-09-01T09:30:00Z", "110", 100L, "1", "2");
        var decrease = engine.analyze(snapshot("2026-09-01T10:00:00Z", "100", 100L, "1", "0"), previous, List.of(previous));
        var unchanged = engine.analyze(snapshot("2026-09-01T10:00:00Z", "110", 100L, "1", "0"), previous, List.of(previous));

        assertThat(decrease.price().absoluteChange()).isEqualByComparingTo("-10");
        assertThat(decrease.relativeMovement().interpretation()).isEqualTo("UNDERPERFORMING_SECTOR");
        assertThat(unchanged.price().percentageChange()).isEqualByComparingTo("0.0000000000");
        assertThat(unchanged.relativeMovement().interpretation()).isEqualTo("MATCHING_SECTOR");
    }

    private MarketSnapshot snapshot(String timestamp, String price, long volume, String volatility, String sector) {
        return new MarketSnapshot("TEST", Instant.parse(timestamp), new BigDecimal(price), volume,
                new BigDecimal(volatility), new BigDecimal(sector), "TEST");
    }
}