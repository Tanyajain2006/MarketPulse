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

import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;

@Service
public class MarketDataImportService {
    private static final Logger log = LoggerFactory.getLogger(MarketDataImportService.class);
    private final MarketDataProvider provider;
    private final MarketSnapshotRepository snapshots;
    private final Path configuredLocation;

    public MarketDataImportService(MarketDataProvider provider, MarketSnapshotRepository snapshots,
            @Value("${marketpulse.market-data.csv-location:data/historical-market-data.csv}") String location) {
        this.provider = provider;
        this.snapshots = snapshots;
        this.configuredLocation = Path.of(location);
    }

    @Transactional
    public ImportSummary importConfiguredFile() {
        return importFile(configuredLocation);
    }

    @Transactional
    public ImportSummary importFile(Path location) {
        Instant started = Instant.now();
        try {
            MarketDataBatch batch = provider.load(location);
            int imported = 0;
            int duplicates = 0;
            for (MarketDataBatch.MarketDataPoint row : batch.rows()) {
                if (snapshots.existsByTickerAndObservationTimestamp(row.ticker(), row.observationTimestamp())) {
                    duplicates++;
                    continue;
                }
                try {
                    snapshots.save(new MarketSnapshot(row.ticker(), row.observationTimestamp(), row.price(), row.volume(),
                            row.volatility(), row.sectorChange(), row.source(), "VALID", Instant.now()));
                    imported++;
                } catch (DataIntegrityViolationException exception) {
                    duplicates++;
                }
            }
            ImportSummary summary = new ImportSummary(batch.totalRows(), imported, duplicates, batch.rejectedRows(),
                    Duration.between(started, Instant.now()).toMillis());
            log.info("Historical market data import completed: total={} imported={} duplicates={} rejected={} duration={}ms",
                    summary.totalRows(), summary.importedRows(), summary.duplicateRows(), summary.rejectedRows(), summary.durationMillis());
            return summary;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read historical market data from " + location, exception);
        }
    }

    public record ImportSummary(int totalRows, int importedRows, int duplicateRows, int rejectedRows, long durationMillis) { }
}