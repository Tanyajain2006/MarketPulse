package com.marketpulse.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class InvestigationDtos {
    private InvestigationDtos() { }

    public record InvestigationResponse(String ticker, Long watchlistId, String watchlistName,
            String status, Checkpoint checkpoint, MarketState checkpointMarketState, MarketState currentMarketState,
            MarketChangeEngine.PriceResult priceChange, MarketChangeEngine.VolumeResult volumeAnomaly,
            MarketChangeEngine.VolatilityResult volatilityChange, MarketChangeEngine.RelativeMovementResult sectorMovement,
            MarketChangeEngine.PeerComparisonResult peerMovement, MaterialityScoreEngine.MaterialityResult materiality,
            List<NewsArticleDtos.ArticleResponse> news, Availability sentiment, NarrativeShift narrativeShift,
            List<EventTypeSummary> eventTypes, DataQuality dataQuality, InvestigationGraph relationships) { }

    public record Checkpoint(Instant reviewedAt, String status) { }
    public record MarketState(Instant observationTimestamp, BigDecimal price, Long volume, BigDecimal volatility,
            BigDecimal sectorChange, String source, String dataQuality) { }
    public record Availability(String status, BigDecimal value) { }
    public record NarrativeShift(String status, String previousNarrative, String currentNarrative, String change) { }
    public record EventTypeSummary(NewsEventType eventType, int articleCount) { }
    public record DataQuality(String checkpoint, String current, String news, List<String> warnings) { }
    public record InvestigationGraph(List<InvestigationNode> nodes, List<InvestigationEdge> edges) { }
    public record InvestigationNode(String id, String type, String label, Map<String, String> metadata) { }
    public record InvestigationEdge(String source, String target, String relationshipType, String label) { }
}