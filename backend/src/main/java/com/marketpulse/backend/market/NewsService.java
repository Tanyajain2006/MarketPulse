package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsService {
    private final NewsEventRepository events;
    private final MarketService market;

    public NewsService(NewsEventRepository events, MarketService market) {
        this.events = events;
        this.market = market;
    }

    @Transactional(readOnly = true)
    public List<MarketSnapshotDtos.NewsResponse> byTicker(String ticker, Instant from, Instant to, String eventType,
            int limit) {
        String normalized = market.normalizeTicker(ticker);
        List<NewsEvent> values;
        if (from == null && to == null && eventType == null) {
            values = events.findByTickerOrderByTimestampDesc(normalized, PageRequest.of(0, limit));
        } else {
            values = events.findFiltered(normalized, from == null ? Instant.EPOCH : from,
                    to == null ? Instant.now() : to, eventType, PageRequest.of(0, limit));
        }
        return values.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<MarketSnapshotDtos.NewsResponse> latest(List<String> tickers, int limit) {
        List<String> normalized = tickers.stream().map(market::normalizeTicker).distinct().toList();
        return events.findLatestForTickers(normalized, Instant.EPOCH, PageRequest.of(0, limit)).stream()
                .map(this::response).toList();
    }

    private MarketSnapshotDtos.NewsResponse response(NewsEvent event) {
        return new MarketSnapshotDtos.NewsResponse(event.getTimestamp(), event.getTicker(), event.getHeadline(),
                event.getSource(), event.getEventType(), event.getSentimentScore());
    }
}
