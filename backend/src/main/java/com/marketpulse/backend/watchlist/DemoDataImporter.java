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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.market.NewsEvent;
import com.marketpulse.backend.market.NewsEventRepository;
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
    private final NewsEventRepository news;
    private final PasswordEncoder passwordEncoder;
    private final String dataDirectory;
    private final String demoEmail;
    private final String demoPassword;

    public DemoDataImporter(UserRepository users, WatchlistRepository watchlists, WatchlistItemRepository items,
            InstrumentRepository instruments, MarketSnapshotRepository snapshots,
            NewsEventRepository news, PasswordEncoder passwordEncoder, org.springframework.core.env.Environment environment) {
        this.users = users;
        this.watchlists = watchlists;
        this.items = items;
        this.instruments = instruments;
        this.snapshots = snapshots;
        this.news = news;
        this.passwordEncoder = passwordEncoder;
        this.dataDirectory = environment.getProperty("app.demo-data.directory", "../data");
        this.demoEmail = environment.getProperty("app.demo-data.user-email", "demo@marketpulse.local");
        this.demoPassword = environment.getProperty("app.demo-data.password", "marketpulse-demo-local");
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        importInstruments();
        importSnapshots();
        importNews();
        if (!demoEmail.isBlank()) attachWatchlistsToConfiguredUser();
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
            if (!snapshots.existsByTickerAndObservationTimestamp(ticker, timestamp)) {
                snapshots.save(new MarketSnapshot(ticker, timestamp, new BigDecimal(row[2]), Long.valueOf(row[3]),
                        new BigDecimal(row[4]), new BigDecimal(row[5]), row[6].trim()));
            }
        }
    }

    private void importNews() throws IOException {
        Path path = Path.of(dataDirectory, "news_events.csv");
        for (String[] row : rows(path)) {
            if (row.length < 6) continue;
            String ticker = row[1].trim().toUpperCase();
            var timestamp = LocalDateTime.parse(row[0].trim().replace(' ', 'T')).toInstant(ZoneOffset.UTC);
            String headline = row[2].trim();
            if (!news.existsByTickerAndTimestampAndHeadline(ticker, timestamp, headline)) {
                news.save(new NewsEvent(ticker, timestamp, headline, row[3].trim(), row[4].trim(), new BigDecimal(row[5])));
            }
        }
    }

    private void attachWatchlistsToConfiguredUser() throws IOException {
        User user = users.findByEmail(demoEmail.trim().toLowerCase()).orElseGet(() ->
                users.save(new User("MarketPulse Demo", demoEmail.trim().toLowerCase(), passwordEncoder.encode(demoPassword))));
        List<String[]> seedRows = rows(Path.of(dataDirectory, "watchlist_seed.csv"));
        if (seedRows.isEmpty()) return;
        Map<String, Watchlist> targets = new LinkedHashMap<>();
        for (String[] row : seedRows) {
            if (row.length < 4) continue;
            String sourceName = row[0].trim();
            Watchlist targetWatchlist = targets.computeIfAbsent(sourceName, name ->
                    watchlists.findByUserEmailAndName(user.getEmail(), name).orElseGet(() -> watchlists.save(new Watchlist(user, name))));
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