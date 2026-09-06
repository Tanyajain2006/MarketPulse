package com.marketpulse.backend.watchlist;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataImporter implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataImporter.class);
    private final UserRepository users;
    private final WatchlistRepository watchlists;
    private final WatchlistItemRepository items;
    private final InstrumentRepository instruments;
    private final MarketSnapshotRepository snapshots;
    private final String dataDirectory;
    private final String demoEmail;
    private final String demoWatchlistName;

    public DemoDataImporter(UserRepository users, WatchlistRepository watchlists, WatchlistItemRepository items,
            InstrumentRepository instruments, MarketSnapshotRepository snapshots,
            org.springframework.core.env.Environment environment) {
        this.users = users;
        this.watchlists = watchlists;
        this.items = items;
        this.instruments = instruments;
        this.snapshots = snapshots;
        this.dataDirectory = environment.getProperty("app.demo-data.directory", "../data");
        this.demoEmail = environment.getProperty("app.demo-data.user-email", "");
        this.demoWatchlistName = environment.getProperty("app.demo-data.watchlist-name", "My Tech Watchlist");
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        importInstruments();
        importSnapshots();
        if (!demoEmail.isBlank()) attachWatchlistToConfiguredUser();
        log.info("MarketPulse demo data import completed from {}", dataDirectory);
    }

    private void importInstruments() throws IOException {
        Path path = Path.of(dataDirectory, "watchlist_seed.csv");
        Map<String, Instrument> catalog = new HashMap<>();
        for (String[] row : rows(path)) {
            if (row.length < 4) continue;
            catalog.put(row[1].trim().toUpperCase(), new Instrument(row[1].trim().toUpperCase(), row[2].trim(), row[3].trim()));
        }
        catalog.values().forEach(instrument -> instruments.save(instrument));
    }

    private void importSnapshots() throws IOException {
        Path path = Path.of(dataDirectory, "market_snapshots.csv");
        for (String[] row : rows(path)) {
            if (row.length < 7) continue;
            String ticker = row[1].trim().toUpperCase();
            var timestamp = LocalDateTime.parse(row[0].trim().replace(' ', 'T')).toInstant(ZoneOffset.UTC);
            if (!snapshots.existsByTickerAndTimestamp(ticker, timestamp)) {
                snapshots.save(new MarketSnapshot(ticker, timestamp, new BigDecimal(row[2]), Long.valueOf(row[3]),
                        new BigDecimal(row[4]), new BigDecimal(row[5]), row[6].trim()));
            }
        }
    }

    private void attachWatchlistToConfiguredUser() throws IOException {
        User user = users.findByEmail(demoEmail.trim().toLowerCase()).orElse(null);
        if (user == null) {
            log.info("Skipping demo watchlist attachment; configured user {} does not exist yet", demoEmail);
            return;
        }
        List<String[]> seedRows = rows(Path.of(dataDirectory, "watchlist_seed.csv"));
        if (seedRows.isEmpty()) return;
        Watchlist watchlist = watchlists.findAllByUserEmailOrderByUpdatedAtDesc(user.getEmail()).stream()
                .filter(item -> item.getName().equals(demoWatchlistName)).findFirst().orElse(null);
        if (watchlist == null) watchlist = watchlists.save(new Watchlist(user, demoWatchlistName));
        final Watchlist targetWatchlist = watchlist;
        String sourceName = seedRows.get(0)[0];
        for (String[] row : seedRows) {
            if (row.length < 4 || !sourceName.equals(row[0])) continue;
            String ticker = row[1].trim().toUpperCase();
            if (!items.existsByWatchlistIdAndTicker(targetWatchlist.getId(), ticker)) {
                instruments.findByTickerIgnoreCase(ticker).ifPresent(instrument -> items.save(new WatchlistItem(targetWatchlist, instrument)));
            }
        }
    }

    private List<String[]> rows(Path path) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) result.add(parse(line));
        }
        return result;
    }

    private String[] parse(String line) {
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