package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.watchlist.Instrument;
import com.marketpulse.backend.watchlist.InstrumentRepository;
import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class MarketService {
    private final MarketSnapshotRepository snapshots;
    private final InstrumentRepository instruments;

    public MarketService(MarketSnapshotRepository snapshots, InstrumentRepository instruments) {
        this.snapshots = snapshots;
        this.instruments = instruments;
    }

    @Transactional(readOnly = true)
    public List<MarketSnapshotDtos.SnapshotResponse> latest(List<String> tickers) {
        List<String> normalized = normalizeTickers(tickers);
        return snapshots.findLatestForTickers(normalized).stream().map(this::snapshot).toList();
    }

    @Transactional(readOnly = true)
    public MarketSnapshotDtos.SnapshotResponse latest(String ticker) {
        String normalized = normalizeTicker(ticker);
        return snapshots.findFirstByTickerAndDataQualityOrderByObservationTimestampDesc(normalized, "VALID")
                .map(this::snapshot).orElseThrow(EntityNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<MarketSnapshotDtos.SnapshotResponse> latestTickers() {
        List<String> tickers = snapshots.findDistinctTickers();
        return tickers.isEmpty() ? List.of() : latest(tickers);
    }

    @Transactional(readOnly = true)
    public List<String> availableTickers() { return snapshots.findDistinctTickers(); }

    @Transactional(readOnly = true)
    public List<MarketSnapshotDtos.SnapshotResponse> history(String ticker, Instant from, Instant to, int limit) {
        String normalized = normalizeTicker(ticker);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to.");
        }
        if (from == null && to == null) {
            return snapshots.findByTickerAndDataQualityAndObservationTimestampBetweenOrderByObservationTimestampAsc(
                    normalized, "VALID", java.time.Instant.EPOCH, java.time.Instant.now(), PageRequest.of(0, limit))
                    .stream().map(this::snapshot).toList();
        }
        Instant effectiveFrom = from == null ? Instant.EPOCH : from;
        Instant effectiveTo = to == null ? Instant.now() : to;
        return snapshots.findByTickerAndDataQualityAndObservationTimestampBetweenOrderByObservationTimestampAsc(
                normalized, "VALID", effectiveFrom, effectiveTo, PageRequest.of(0, limit)).stream().map(this::snapshot).toList();
    }

    @Transactional(readOnly = true)
    public Instrument instrument(String ticker) {
        return instruments.findByTickerIgnoreCase(normalizeTicker(ticker)).orElseThrow(EntityNotFoundException::new);
    }

    private MarketSnapshotDtos.SnapshotResponse snapshot(MarketSnapshot value) {
        return new MarketSnapshotDtos.SnapshotResponse(value.getTimestamp(), value.getTicker(), value.getPrice(),
            value.getVolume(), value.getVolatility(), value.getSectorChange(), value.getSource(),
            value.getIngestionTimestamp(), value.getDataQuality());
    }

    public String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) throw new IllegalArgumentException("Ticker is required.");
        return ticker.trim().toUpperCase();
    }

    private List<String> normalizeTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) throw new IllegalArgumentException("At least one ticker is required.");
        return tickers.stream().map(this::normalizeTicker).distinct().toList();
    }
}
