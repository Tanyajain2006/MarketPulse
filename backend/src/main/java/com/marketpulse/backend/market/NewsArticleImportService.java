package com.marketpulse.backend.market;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsArticleImportService {
    private static final Logger log = LoggerFactory.getLogger(NewsArticleImportService.class);
    private final NewsProvider provider;
    private final NewsArticleRepository articles;
    private final Path configuredLocation;

    public NewsArticleImportService(NewsProvider provider, NewsArticleRepository articles,
            @Value("${marketpulse.news-data.csv-location:data/news_events.csv}") String location) {
        this.provider = provider;
        this.articles = articles;
        this.configuredLocation = Path.of(location);
    }

    @Transactional
    public ImportSummary importConfiguredFile() { return importFile(configuredLocation); }

    @Transactional
    public ImportSummary importFile(Path location) {
        Instant started = Instant.now();
        try {
            NewsDataBatch batch = provider.load(location);
            int imported = 0;
            int duplicates = 0;
            for (NewsDataBatch.NewsDataPoint row : batch.rows()) {
                if (articles.existsByTickerAndPublishedAtAndHeadlineAndSource(row.ticker(), row.publishedAt(), row.headline(), row.source())) {
                    duplicates++;
                    continue;
                }
                try {
                    articles.save(new NewsArticle(row.ticker(), row.headline(), row.source(), row.publishedAt(),
                            Instant.now(), row.eventType()));
                    imported++;
                } catch (DataIntegrityViolationException exception) {
                    duplicates++;
                }
            }
            ImportSummary summary = new ImportSummary(batch.totalRows(), imported, duplicates, batch.rejectedRows(),
                    Duration.between(started, Instant.now()).toMillis());
            log.info("Historical news import completed: total={} imported={} duplicates={} rejected={} duration={}ms",
                    summary.totalRows(), summary.importedRows(), summary.duplicateRows(), summary.rejectedRows(), summary.durationMillis());
            return summary;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read historical news from " + location, exception);
        }
    }

    public record ImportSummary(int totalRows, int importedRows, int duplicateRows, int rejectedRows, long durationMillis) { }
}