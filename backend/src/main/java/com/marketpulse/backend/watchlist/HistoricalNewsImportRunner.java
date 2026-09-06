package com.marketpulse.backend.watchlist;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.marketpulse.backend.market.NewsArticleImportService;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class HistoricalNewsImportRunner implements CommandLineRunner {
    private final NewsArticleImportService importer;

    public HistoricalNewsImportRunner(NewsArticleImportService importer) { this.importer = importer; }

    @Override
    public void run(String... args) { importer.importConfiguredFile(); }
}