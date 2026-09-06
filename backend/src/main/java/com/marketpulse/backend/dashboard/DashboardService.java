package com.marketpulse.backend.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.market.NewsEvent;
import com.marketpulse.backend.market.NewsEventRepository;
import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;
import com.marketpulse.backend.watchlist.Instrument;
import com.marketpulse.backend.watchlist.InstrumentRepository;
import com.marketpulse.backend.watchlist.MarketSnapshot;
import com.marketpulse.backend.watchlist.MarketSnapshotRepository;
import com.marketpulse.backend.watchlist.Watchlist;
import com.marketpulse.backend.watchlist.WatchlistRepository;

@Service
public class DashboardService {
    private final UserRepository users;
    private final WatchlistRepository watchlists;
    private final UserCheckpointRepository checkpoints;
    private final MarketSnapshotRepository snapshots;
    private final NewsEventRepository news;
    private final InstrumentRepository instruments;

    public DashboardService(UserRepository users, WatchlistRepository watchlists, UserCheckpointRepository checkpoints,
            MarketSnapshotRepository snapshots, NewsEventRepository news, InstrumentRepository instruments) {
        this.users = users;
        this.watchlists = watchlists;
        this.checkpoints = checkpoints;
        this.snapshots = snapshots;
        this.news = news;
        this.instruments = instruments;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.Overview overview(String email) {
        Watchlist watchlist = watchlists.findAllByUserEmailOrderByUpdatedAtDesc(email).stream().findFirst()
                .orElse(null);
        Instant checkpoint = checkpoints.findByUserEmail(email).map(UserCheckpoint::getObservedAt).orElse(Instant.EPOCH);
        if (watchlist == null || watchlist.getItems().isEmpty()) {
            return new DashboardDtos.Overview(watchlist == null ? null : watchlist.getName(), checkpoint, null,
                    new DashboardDtos.Summary(0, 0, 0, 0), List.of());
        }
        List<DashboardDtos.Change> changes = new ArrayList<>();
        for (var item : watchlist.getItems()) {
            List<MarketSnapshot> rows = snapshots.findSinceForTicker(item.getTicker(), checkpoint, PageRequest.of(0, 25));
            if (rows.isEmpty()) continue;
            MarketSnapshot current = rows.get(0);
            MarketSnapshot previous = rows.size() > 1 ? rows.get(1) : previous(item.getTicker(), current);
            NewsEvent context = news.findFiltered(item.getTicker(), checkpoint, current.getTimestamp(), null, PageRequest.of(0, 1))
                    .stream().findFirst().orElse(null);
            changes.add(change(item.getTicker(), current, previous, context));
        }
        changes.sort(Comparator.comparingInt(DashboardDtos.Change::materiality).reversed());
        int critical = (int) changes.stream().filter(change -> "CRITICAL".equals(change.classification())).count();
        int meaningful = (int) changes.stream().filter(change -> "MEANINGFUL".equals(change.classification())).count();
        int normal = (int) changes.stream().filter(change -> "NORMAL".equals(change.classification())).count();
        Instant latest = changes.stream().map(DashboardDtos.Change::observedAt).max(Comparator.naturalOrder()).orElse(null);
        return new DashboardDtos.Overview(watchlist.getName(), checkpoint, latest,
                new DashboardDtos.Summary(critical, meaningful, normal, meaningful + critical), changes);
    }

    @Transactional
    public Instant checkpoint(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found."));
        Instant now = Instant.now();
        UserCheckpoint value = checkpoints.findByUserEmail(email).orElseGet(() -> new UserCheckpoint(user, now));
        value.observe(now);
        checkpoints.save(value);
        return now;
    }

    private MarketSnapshot previous(String ticker, MarketSnapshot current) {
        return snapshots.findTop2ByTickerOrderByTimestampDesc(ticker).stream()
                .filter(value -> value.getTimestamp().isBefore(current.getTimestamp())).findFirst().orElse(null);
    }

    private DashboardDtos.Change change(String ticker, MarketSnapshot current, MarketSnapshot previous, NewsEvent context) {
        BigDecimal priceChange = previous == null || previous.getPrice().signum() == 0 ? BigDecimal.ZERO
                : current.getPrice().subtract(previous.getPrice()).divide(previous.getPrice(), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal volumeMultiple = previous == null || previous.getVolume() == 0 ? BigDecimal.ONE
                : BigDecimal.valueOf(current.getVolume()).divide(BigDecimal.valueOf(previous.getVolume()), 2, RoundingMode.HALF_UP);
        int materiality = materiality(priceChange, volumeMultiple, current.getVolatility(), current.getSectorChange(), context);
        Instrument instrument = instruments.findByTickerIgnoreCase(ticker).orElse(null);
        String classification = materiality >= 75 ? "CRITICAL" : materiality >= 50 ? "MEANINGFUL" : materiality >= 25 ? "WATCHING" : "NORMAL";
        return new DashboardDtos.Change(ticker, instrument == null ? null : instrument.getCompanyName(), instrument == null ? null : instrument.getExchange(),
                current.getTimestamp(), current.getPrice(), priceChange, current.getVolume(), volumeMultiple,
                current.getVolatility(), current.getSectorChange(), classification, materiality,
                context == null ? null : context.getHeadline(), context == null ? null : context.getEventType(),
                context == null ? null : context.getSentimentScore());
    }

    private int materiality(BigDecimal priceChange, BigDecimal volumeMultiple, BigDecimal volatility,
            BigDecimal sectorChange, NewsEvent context) {
        double score = Math.min(45, priceChange.abs().doubleValue() * 8)
                + Math.min(20, Math.max(0, volumeMultiple.doubleValue() - 1) * 8)
                + Math.min(15, volatility.abs().doubleValue() * 3)
                + Math.min(10, sectorChange.abs().doubleValue() * 2);
        if (context != null) score += Math.min(10, Math.abs(context.getSentimentScore().doubleValue()) * 10);
        return Math.min(100, (int) Math.round(score));
    }
}
