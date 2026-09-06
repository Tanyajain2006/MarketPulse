package com.marketpulse.backend.market;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HistoricalCsvNewsProvider implements NewsProvider {
    private static final Logger log = LoggerFactory.getLogger(HistoricalCsvNewsProvider.class);
    private static final String HEADER = "timestamp,ticker,headline,source,event_type";

    @Override
    public NewsDataBatch load(Path location) throws IOException {
        List<NewsDataBatch.NewsDataPoint> rows = new ArrayList<>();
        int total = 0;
        int rejected = 0;
        try (BufferedReader reader = java.nio.file.Files.newBufferedReader(location)) {
            String header = reader.readLine();
            if (header == null || !header.equalsIgnoreCase(HEADER)
                    && !header.equalsIgnoreCase(HEADER + ",sentiment_score")) {
                throw new IOException("Expected CSV header: " + HEADER);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                try {
                    rows.add(parse(line));
                } catch (RuntimeException exception) {
                    rejected++;
                    if (rejected <= 10) log.warn("Rejected historical news row {}: {}", total + 1, exception.getMessage());
                }
            }
        }
        return new NewsDataBatch(rows, total, rejected);
    }

    private NewsDataBatch.NewsDataPoint parse(String line) {
        String[] fields = split(line);
        if (fields.length != 5 && fields.length != 6) throw new IllegalArgumentException("expected 5 fields");
        String ticker = fields[1].trim().toUpperCase();
        String headline = fields[2].trim();
        String source = fields[3].trim();
        if (ticker.isEmpty()) throw new IllegalArgumentException("ticker is required");
        if (!ticker.matches("[A-Z0-9.-]+")) throw new IllegalArgumentException("ticker is invalid");
        if (headline.isEmpty()) throw new IllegalArgumentException("headline is required");
        if (source.isEmpty()) throw new IllegalArgumentException("source is required");
        Instant timestamp = parseTimestamp(fields[0].trim());
        NewsEventType eventType = parseEventType(fields[4]);
        return new NewsDataBatch.NewsDataPoint(ticker, headline, source, timestamp, eventType);
    }

    private NewsEventType parseEventType(String value) {
        String normalized = value.trim().toUpperCase();
        if (normalized.isEmpty()) throw new IllegalArgumentException("event_type is required");
        try {
            return NewsEventType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            if (normalized.equals("COMPANY_NEWS")) return NewsEventType.OTHER;
            throw new IllegalArgumentException("unknown event_type: " + value);
        }
    }

    private Instant parseTimestamp(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("timestamp is not parseable");
            }
        }
    }

    private String[] split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') quoted = !quoted;
            else if (character == ',' && !quoted) { fields.add(field.toString()); field.setLength(0); }
            else field.append(character);
        }
        fields.add(field.toString());
        return fields.toArray(String[]::new);
    }
}