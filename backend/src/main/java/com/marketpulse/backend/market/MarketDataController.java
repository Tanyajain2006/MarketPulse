package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {
    private final MarketService market;

    public MarketDataController(MarketService market) { this.market = market; }

    @GetMapping
    public List<MarketSnapshotDtos.SnapshotResponse> history(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {
        if (ticker == null || ticker.isBlank()) return market.latestTickers();
        return market.history(ticker, from, to, Math.min(Math.max(limit, 1), 1000));
    }

    @GetMapping("/tickers")
    public List<String> tickers() { return market.availableTickers(); }
}