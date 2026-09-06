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

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService news;

    public NewsController(NewsService news) { this.news = news; }

    @GetMapping("/latest")
    public List<MarketSnapshotDtos.NewsResponse> latest(@RequestParam String tickers,
            @RequestParam(defaultValue = "50") int limit) {
        return news.latest(Arrays.asList(tickers.split(",")), Math.min(Math.max(limit, 1), 500));
    }

    @GetMapping("/{ticker}")
    public List<MarketSnapshotDtos.NewsResponse> byTicker(@PathVariable String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "50") int limit) {
        return news.byTicker(ticker, from, to, eventType, Math.min(Math.max(limit, 1), 500));
    }
}
