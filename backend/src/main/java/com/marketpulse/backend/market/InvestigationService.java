package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.dashboard.UserCheckpoint;
import com.marketpulse.backend.dashboard.UserCheckpointRepository;
import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;
import com.marketpulse.backend.watchlist.Watchlist;
import com.marketpulse.backend.watchlist.WatchlistRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class InvestigationService {
    private static final String VALID = "VALID";
    private final WatchlistRepository watchlists;
    private final UserCheckpointRepository checkpoints;
    private final MarketSnapshotRepository snapshots;
    private final NewsArticleRepository articles;
    private final MarketChangeEngine changes;
    private final MaterialityScoreEngine materiality;
    private final int baselineWindow;

    public InvestigationService(WatchlistRepository watchlists, UserCheckpointRepository checkpoints,
            MarketSnapshotRepository snapshots, NewsArticleRepository articles, MarketChangeEngine changes,
            MaterialityScoreEngine materiality,
            @Value("${marketpulse.change-engine.baseline-window:20}") int baselineWindow) {
        this.watchlists = watchlists;
        this.checkpoints = checkpoints;
        this.snapshots = snapshots;
        this.articles = articles;
        this.changes = changes;
        this.materiality = materiality;
        this.baselineWindow = baselineWindow;
    }

    @Transactional(readOnly = true)
    public InvestigationDtos.InvestigationResponse investigate(String email, String ticker, Long watchlistId) {
        if (watchlistId == null || watchlistId <= 0) throw new IllegalArgumentException("watchlistId must be positive.");
        String normalized = normalizeTicker(ticker);
        Watchlist watchlist = watchlists.findByIdAndUserEmail(watchlistId, email).orElseThrow(EntityNotFoundException::new);
        if (watchlist.getItems().stream().noneMatch(item -> normalized.equals(item.getTicker()))) {
            throw new EntityNotFoundException();
        }
        UserCheckpoint checkpoint = checkpoints.findByUserEmailAndWatchlistId(email, watchlistId).orElse(null);
        if (checkpoint == null) return empty(normalized, watchlist, "NO_CHECKPOINT", null);
        MarketSnapshot current = snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc(normalized, VALID).orElse(null);
        if (current == null) return empty(normalized, watchlist, "NO_MARKET_DATA", checkpoint);
        MarketSnapshot previous = snapshots.findFirstByTickerAndDataQualityAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(
                normalized, VALID, checkpoint.getReviewedAt()).orElse(null);
        if (previous == null) return empty(normalized, watchlist, "INSUFFICIENT_HISTORY", checkpoint, current);
        List<MarketSnapshot> prior = snapshots.findByTickerAndDataQualityAndObservationTimestampBeforeOrderByObservationTimestampDesc(
                normalized, VALID, current.getObservationTimestamp(), PageRequest.of(0, baselineWindow));
        MarketChangeEngine.MarketChangeResult result = changes.analyze(current, previous, prior);
        MaterialityScoreEngine.MaterialityResult score = materiality.score(result);
        List<NewsArticle> news = articles.findByTickerAndPublishedAtBetweenOrderByPublishedAtAsc(normalized,
                checkpoint.getReviewedAt(), current.getObservationTimestamp(), PageRequest.of(0, 100));
        return response(normalized, watchlist, checkpoint, previous, current, result, score, news, "CALCULATED");
    }

    private InvestigationDtos.InvestigationResponse response(String ticker, Watchlist watchlist, UserCheckpoint checkpoint,
            MarketSnapshot previous, MarketSnapshot current, MarketChangeEngine.MarketChangeResult result,
            MaterialityScoreEngine.MaterialityResult score, List<NewsArticle> news, String status) {
        List<NewsArticleDtos.ArticleResponse> articleDtos = news.stream().map(article -> new NewsArticleDtos.ArticleResponse(
                article.getId(), article.getTicker(), article.getHeadline(), article.getSource(), article.getPublishedAt(),
                article.getIngestionTimestamp(), article.getEventType(), article.getSentimentScore())).toList();
        List<InvestigationDtos.EventTypeSummary> eventTypes = news.stream().collect(java.util.stream.Collectors.groupingBy(
                article -> article.getEventType(), LinkedHashMap::new, java.util.stream.Collectors.counting())).entrySet().stream()
                .map(entry -> new InvestigationDtos.EventTypeSummary(entry.getKey(), entry.getValue().intValue())).toList();
        List<String> warnings = new ArrayList<>();
        if (current.getObservationTimestamp().isBefore(Instant.now().minus(java.time.Duration.ofHours(24)))) warnings.add("CURRENT_DATA_STALE");
        return new InvestigationDtos.InvestigationResponse(ticker, watchlist.getId(), watchlist.getName(), status,
                new InvestigationDtos.Checkpoint(checkpoint.getReviewedAt(), "AVAILABLE"), state(previous), state(current),
                result.price(), result.volume(), result.volatility(), result.relativeMovement(), result.peerComparison(), score,
                articleDtos, new InvestigationDtos.Availability("NOT_ANALYZED", null),
                new InvestigationDtos.NarrativeShift("NOT_AVAILABLE", null, null, null), eventTypes,
                new InvestigationDtos.DataQuality(previous.getDataQuality(), current.getDataQuality(),
                        news.isEmpty() ? "NO_NEWS" : "AVAILABLE", warnings), graph(ticker, result, score, news));
    }

    private InvestigationDtos.InvestigationResponse empty(String ticker, Watchlist watchlist, String status,
            UserCheckpoint checkpoint) { return empty(ticker, watchlist, status, checkpoint, null); }

    private InvestigationDtos.InvestigationResponse empty(String ticker, Watchlist watchlist, String status,
            UserCheckpoint checkpoint, MarketSnapshot current) {
        return new InvestigationDtos.InvestigationResponse(ticker, watchlist.getId(), watchlist.getName(), status,
                checkpoint == null ? null : new InvestigationDtos.Checkpoint(checkpoint.getReviewedAt(), "AVAILABLE"), null,
                current == null ? null : state(current), null, null, null, null, null, null, List.of(),
                new InvestigationDtos.Availability("NOT_ANALYZED", null),
                new InvestigationDtos.NarrativeShift("NOT_AVAILABLE", null, null, null), List.of(),
                new InvestigationDtos.DataQuality("UNAVAILABLE", current == null ? "MISSING" : current.getDataQuality(), "UNAVAILABLE", List.of(status)),
                new InvestigationDtos.InvestigationGraph(List.of(), List.of()));
    }

    private InvestigationDtos.MarketState state(MarketSnapshot value) {
        return new InvestigationDtos.MarketState(value.getObservationTimestamp(), value.getPrice(), value.getVolume(),
                value.getVolatility(), value.getSectorChange(), value.getSource(), value.getDataQuality());
    }

    private InvestigationDtos.InvestigationGraph graph(String ticker, MarketChangeEngine.MarketChangeResult result,
            MaterialityScoreEngine.MaterialityResult score, List<NewsArticle> news) {
        List<InvestigationDtos.InvestigationNode> nodes = new ArrayList<>();
        List<InvestigationDtos.InvestigationEdge> edges = new ArrayList<>();
        nodes.add(new InvestigationDtos.InvestigationNode("stock-" + ticker, "STOCK", ticker, Map.of()));
        nodes.add(new InvestigationDtos.InvestigationNode("materiality", "MATERIALITY", score.classification().name(),
                Map.of("score", score.score().toString())));
        edges.add(new InvestigationDtos.InvestigationEdge("stock-" + ticker, "materiality", "HAS_MATERIALITY", null));
        if (result.price().status() == MarketChangeEngine.Status.CALCULATED) {
            nodes.add(new InvestigationDtos.InvestigationNode("price-change", "PRICE_CHANGE", "Price change", Map.of()));
            edges.add(new InvestigationDtos.InvestigationEdge("stock-" + ticker, "price-change", "HAS_CHANGE", null));
        }
        if (result.volume().signal() == MarketChangeEngine.Signal.ANOMALOUS) {
            nodes.add(new InvestigationDtos.InvestigationNode("volume-anomaly", "VOLUME_ANOMALY", "Volume anomaly", Map.of()));
            edges.add(new InvestigationDtos.InvestigationEdge("stock-" + ticker, "volume-anomaly", "HAS_ANOMALY", null));
        }
        for (NewsArticle article : news) {
            String id = "news-" + article.getId();
            nodes.add(new InvestigationDtos.InvestigationNode(id, "NEWS_EVENT", article.getHeadline(),
                    Map.of("eventType", article.getEventType().name())));
            edges.add(new InvestigationDtos.InvestigationEdge(id, "stock-" + ticker, "ASSOCIATED_WITH", null));
        }
        return new InvestigationDtos.InvestigationGraph(nodes, edges);
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) throw new IllegalArgumentException("Ticker is required.");
        return ticker.trim().toUpperCase();
    }
}