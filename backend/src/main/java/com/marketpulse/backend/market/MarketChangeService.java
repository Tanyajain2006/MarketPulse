package com.marketpulse.backend.market;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class MarketChangeService {
    private static final String VALID = "VALID";
    private final MarketSnapshotRepository snapshots;
    private final MarketService market;
    private final MarketChangeEngine engine;
    private final int baselineWindow;

    public MarketChangeService(MarketSnapshotRepository snapshots, MarketService market, MarketChangeEngine engine,
            @org.springframework.beans.factory.annotation.Value("${marketpulse.change-engine.baseline-window:20}") int baselineWindow) {
        if (baselineWindow < 1) throw new IllegalArgumentException("Baseline window must be positive.");
        this.snapshots = snapshots;
        this.market = market;
        this.engine = engine;
        this.baselineWindow = baselineWindow;
    }

    @Transactional(readOnly = true)
    public MarketChangeDtos.ChangeResponse changes(String ticker) {
        String normalized = market.normalizeTicker(ticker);
        MarketSnapshot current = snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc(normalized, VALID)
                .orElseThrow(EntityNotFoundException::new);
        List<MarketSnapshot> prior = snapshots.findByTickerAndDataQualityAndObservationTimestampBeforeOrderByObservationTimestampDesc(
                normalized, VALID, current.getObservationTimestamp(), PageRequest.of(0, baselineWindow));
        MarketSnapshot previous = prior.isEmpty() ? null : prior.get(0);
        MarketChangeEngine.MarketChangeResult result = engine.analyze(current, previous, prior);
        return map(current, result);
    }

    private MarketChangeDtos.ChangeResponse map(MarketSnapshot current, MarketChangeEngine.MarketChangeResult result) {
        var price = result.price();
        var volume = result.volume();
        var volatility = result.volatility();
        var relative = result.relativeMovement();
        return new MarketChangeDtos.ChangeResponse(current.getTicker(), current.getObservationTimestamp().toString(),
                new MarketChangeDtos.Price(price.previousPrice(), price.currentPrice(), price.absoluteChange(), price.percentageChange(), price.status(), price.reason()),
                new MarketChangeDtos.Volume(volume.currentVolume(), volume.historicalBaseline(), volume.multiple(), volume.threshold(), volume.signal(), volume.status(), volume.reason()),
                new MarketChangeDtos.Volatility(volatility.previousVolatility(), volatility.currentVolatility(), volatility.relativeChange(), volatility.threshold(), volatility.signal(), volatility.status(), volatility.reason()),
                new MarketChangeDtos.RelativeMovement(relative.stockMovement(), relative.sectorMovement(), relative.relativeMovement(), relative.status(), relative.interpretation()),
                new MarketChangeDtos.PeerComparison(result.peerComparison().status(), result.peerComparison().reason()));
    }
}