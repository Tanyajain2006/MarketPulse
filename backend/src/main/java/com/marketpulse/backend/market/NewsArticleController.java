package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/{ticker}/news")
public class NewsArticleController {
    private final NewsArticleService news;
    private final NewsArticleImportService importer;

    public NewsArticleController(NewsArticleService news, NewsArticleImportService importer) {
        this.news = news;
        this.importer = importer;
    }

    @GetMapping
    public List<NewsArticleDtos.ArticleResponse> byTicker(@PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {
        return news.byTicker(ticker, from, to, Math.min(Math.max(limit, 1), 500));
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.OK)
    public NewsArticleImportService.ImportSummary importHistory() {
        return importer.importConfiguredFile();
    }
}