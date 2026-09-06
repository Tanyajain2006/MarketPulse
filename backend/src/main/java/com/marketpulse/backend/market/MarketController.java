package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketService market;

    public MarketController(MarketService market) { this.market = market; }

    @GetMapping("/latest")
    public List<MarketSnapshotDtos.SnapshotResponse> latest(@RequestParam String tickers) {
        return market.latest(Arrays.asList(tickers.split(",")));
    }

    @GetMapping("/{ticker}")
    public MarketSnapshotDtos.SnapshotResponse current(@PathVariable String ticker) {
        return market.latest(List.of(ticker)).stream().findFirst().orElseThrow(EntityNotFoundException::new);
    }

    @GetMapping("/{ticker}/history")
    public List<MarketSnapshotDtos.SnapshotResponse> history(@PathVariable String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {
        return market.history(ticker, from, to, Math.min(Math.max(limit, 1), 1000));
    }
}
