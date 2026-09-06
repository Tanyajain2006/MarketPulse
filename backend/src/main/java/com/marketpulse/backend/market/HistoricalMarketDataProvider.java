package com.marketpulse.backend.market;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
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
public class HistoricalMarketDataProvider implements MarketDataProvider {
    private static final Logger log = LoggerFactory.getLogger(HistoricalMarketDataProvider.class);
    private static final String SOURCE = "HISTORICAL_CSV";
    private static final String HEADER = "timestamp,ticker,price,volume,volatility,sector_change";

    @Override
    public MarketDataBatch load(Path location) throws IOException {
        List<MarketDataBatch.MarketDataPoint> rows = new ArrayList<>();
        int total = 0;
        int rejected = 0;
        try (BufferedReader reader = Files.newBufferedReader(location)) {
            String header = reader.readLine();
            if (header == null || !HEADER.equalsIgnoreCase(header.trim())) {
                throw new IOException("Expected CSV header: " + HEADER);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                try {
                    rows.add(parse(line));
                } catch (RuntimeException exception) {
                    rejected++;
                    if (rejected <= 10) log.warn("Rejected historical market-data row {}: {}", total + 1, exception.getMessage());
                }
            }
        }
        return new MarketDataBatch(rows, total, rejected);
    }

    private MarketDataBatch.MarketDataPoint parse(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != 6) throw new IllegalArgumentException("expected 6 fields");
        String ticker = fields[1].trim().toUpperCase();
        if (ticker.isEmpty()) throw new IllegalArgumentException("ticker is required");
        Instant timestamp = parseTimestamp(fields[0].trim());
        BigDecimal price = decimal(fields[2], "price");
        long volume = Long.parseLong(fields[3].trim());
        if (volume < 0) throw new IllegalArgumentException("volume must not be negative");
        BigDecimal volatility = decimal(fields[4], "volatility");
        BigDecimal sectorChange = decimal(fields[5], "sector_change");
        if (price.signum() < 0 || volatility.signum() < 0) throw new IllegalArgumentException("price and volatility must not be negative");
        return new MarketDataBatch.MarketDataPoint(ticker, timestamp, price, volume, volatility, sectorChange, SOURCE);
    }

    private BigDecimal decimal(String value, String field) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " is not numeric");
        }
    }

    private Instant parseTimestamp(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("timestamp is not parseable");
            }
        }
    }
}