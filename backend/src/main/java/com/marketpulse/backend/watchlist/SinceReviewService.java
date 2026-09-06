package com.marketpulse.backend.watchlist;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.dashboard.UserCheckpoint;
import com.marketpulse.backend.dashboard.UserCheckpointRepository;
import com.marketpulse.backend.market.MarketChangeEngine;
import com.marketpulse.backend.market.MaterialityScoreEngine;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SinceReviewService {
    private static final String VALID = "VALID";
    private final WatchlistRepository watchlists;
    private final UserCheckpointRepository checkpoints;
    private final MarketSnapshotRepository snapshots;
    private final MarketChangeEngine changes;
    private final MaterialityScoreEngine materiality;
    private final int baselineWindow;
    private final long staleAfterHours;

    public SinceReviewService(WatchlistRepository watchlists, UserCheckpointRepository checkpoints,
            MarketSnapshotRepository snapshots, MarketChangeEngine changes, MaterialityScoreEngine materiality,
            @Value("${marketpulse.change-engine.baseline-window:20}") int baselineWindow,
            @Value("${marketpulse.change-engine.stale-after-hours:24}") long staleAfterHours) {
        this.watchlists = watchlists;
        this.checkpoints = checkpoints;
        this.snapshots = snapshots;
        this.changes = changes;
        this.materiality = materiality;
        this.baselineWindow = baselineWindow;
        this.staleAfterHours = staleAfterHours;
    }

    @Transactional(readOnly = true)
    public SinceReviewDtos.SinceReviewResponse sinceLastReview(String email, Long watchlistId) {
        Watchlist watchlist = watchlists.findByIdAndUserEmail(watchlistId, email).orElseThrow(EntityNotFoundException::new);
        UserCheckpoint checkpoint = checkpoints.findByUserEmailAndWatchlistId(email, watchlistId).orElse(null);
                if (checkpoint == null) return new SinceReviewDtos.SinceReviewResponse(watchlistId, watchlist.getName(), null, null,
                                "NO_MARKET_DATA", "NO_CHECKPOINT", new SinceReviewDtos.Summary(0, 0, 0, 0), List.of());
        List<SinceReviewDtos.StockChange> stocks = watchlist.getItems().stream().map(item -> stock(item.getTicker(), checkpoint.getReviewedAt())).toList();
                Instant latest = stocks.stream().map(stock -> stock.currentObservationTimestamp()).filter(value -> value != null).max(Comparator.naturalOrder()).orElse(null);
                int critical = count(stocks, MaterialityScoreEngine.Classification.CRITICAL);
                int meaningful = count(stocks, MaterialityScoreEngine.Classification.MEANINGFUL);
                int watching = count(stocks, MaterialityScoreEngine.Classification.WORTH_WATCHING);
                int normal = count(stocks, MaterialityScoreEngine.Classification.NORMAL);
                String marketStatus = latest == null ? "NO_MARKET_DATA" : stocks.stream().anyMatch(stock -> stock.warning() != null) ? "STALE_DATA" : "HISTORICAL_DATA_AVAILABLE";
                return new SinceReviewDtos.SinceReviewResponse(watchlistId, watchlist.getName(), checkpoint.getReviewedAt(), latest,
                                marketStatus, "CALCULATED", new SinceReviewDtos.Summary(critical, meaningful, watching, normal), stocks);
    }

        private int count(List<SinceReviewDtos.StockChange> stocks, MaterialityScoreEngine.Classification classification) {
                return (int) stocks.stream().filter(stock -> classification == stock.classification()).count();
        }

    private SinceReviewDtos.StockChange stock(String ticker, Instant reviewedAt) {
        var current = snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc(ticker, VALID).orElse(null);
        if (current == null) return unavailable(ticker, "NO_MARKET_DATA", MarketChangeEngine.Status.MISSING_DATA);
        var previous = snapshots.findFirstByTickerAndDataQualityAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(ticker, VALID, reviewedAt).orElse(null);
        if (previous == null) return unavailable(ticker, "INSUFFICIENT_HISTORY", MarketChangeEngine.Status.INSUFFICIENT_HISTORY);
        List<MarketSnapshot> prior = snapshots.findByTickerAndDataQualityAndObservationTimestampBeforeOrderByObservationTimestampDesc(
                ticker, VALID, current.getObservationTimestamp(), PageRequest.of(0, baselineWindow));
        MarketChangeEngine.MarketChangeResult result = changes.analyze(current, previous, prior);
        MaterialityScoreEngine.MaterialityResult score = materiality.score(result);
        String warning = current.getObservationTimestamp().isBefore(Instant.now().minus(Duration.ofHours(staleAfterHours)))
                ? "Latest historical observation is stale." : null;
        return new SinceReviewDtos.StockChange(ticker, previous.getObservationTimestamp(), current.getObservationTimestamp(),
                overall(result), score.score(), score.classification(), warning, result.price(), result.volume(),
                result.volatility(), result.relativeMovement(), result.peerComparison(), score.contributions());
    }

    private SinceReviewDtos.StockChange unavailable(String ticker, String warning, MarketChangeEngine.Status status) {
        return new SinceReviewDtos.StockChange(ticker, null, null, status, null, null, warning, null, null, null, null, null, List.of());
    }

    private MarketChangeEngine.Status overall(MarketChangeEngine.MarketChangeResult result) {
        if (result.price().status() == MarketChangeEngine.Status.INVALID_INPUT
                || result.volume().status() == MarketChangeEngine.Status.INVALID_INPUT
                || result.volatility().status() == MarketChangeEngine.Status.INVALID_INPUT) return MarketChangeEngine.Status.INVALID_INPUT;
        if (result.price().status() == MarketChangeEngine.Status.MISSING_DATA
                || result.volatility().status() == MarketChangeEngine.Status.MISSING_DATA) return MarketChangeEngine.Status.MISSING_DATA;
        if (result.price().status() == MarketChangeEngine.Status.INSUFFICIENT_HISTORY
                || result.volume().status() == MarketChangeEngine.Status.INSUFFICIENT_HISTORY) return MarketChangeEngine.Status.INSUFFICIENT_HISTORY;
        return MarketChangeEngine.Status.CALCULATED;
    }
}