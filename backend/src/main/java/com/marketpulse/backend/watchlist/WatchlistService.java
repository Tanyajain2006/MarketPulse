package com.marketpulse.backend.watchlist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;
import com.marketpulse.backend.watchlist.WatchlistDtos.AddWatchlistItemRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.CreateWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.RenameWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.WatchlistItemResponse;
import com.marketpulse.backend.watchlist.WatchlistDtos.WatchlistResponse;

import jakarta.persistence.EntityNotFoundException;

@Service
public class WatchlistService {
    private final WatchlistRepository watchlists;
    private final WatchlistItemRepository items;
    private final UserRepository users;
        private final InstrumentRepository instruments;
        private final MarketSnapshotRepository snapshots;

        public WatchlistService(WatchlistRepository watchlists, WatchlistItemRepository items, UserRepository users,
            InstrumentRepository instruments, MarketSnapshotRepository snapshots) {
        this.watchlists = watchlists;
        this.items = items;
        this.users = users;
        this.instruments = instruments;
        this.snapshots = snapshots;
    }

    @Transactional
    public WatchlistResponse create(String email, CreateWatchlistRequest request) {
        User user = users.findByEmail(email).orElseThrow(WatchlistNotFoundException::new);
        return response(watchlists.save(new Watchlist(user, cleanName(request.name()))));
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> list(String email) {
        return watchlists.findAllByUserEmailOrderByUpdatedAtDesc(email).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public WatchlistResponse get(String email, Long id) { return response(findOwned(email, id)); }

    @Transactional
    public WatchlistResponse rename(String email, Long id, RenameWatchlistRequest request) {
        Watchlist watchlist = findOwned(email, id);
        watchlist.rename(cleanName(request.name()));
        return response(watchlist);
    }

    @Transactional
    public void delete(String email, Long id) {
        findOwned(email, id);
        watchlists.deleteById(Objects.requireNonNull(id));
    }

    @Transactional
    public WatchlistResponse addItem(String email, Long id, AddWatchlistItemRequest request) {
        Watchlist watchlist = findOwned(email, id);
        String ticker = cleanTicker(request.ticker());
        if (items.existsByWatchlistIdAndTicker(id, ticker)) throw new DuplicateTickerException();
        Instrument instrument = instruments.findByTickerIgnoreCase(ticker).orElseThrow(EntityNotFoundException::new);
        WatchlistItem item = new WatchlistItem(watchlist, instrument);
        try {
            watchlist.getItems().add(items.saveAndFlush(item));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateTickerException();
        }
        watchlist.touch();
        return response(watchlist);
    }

    @Transactional(readOnly = true)
    public List<WatchlistDtos.MarketDataResponse> marketData(String email, Long id) {
        Watchlist watchlist = findOwned(email, id);
        List<String> tickers = watchlist.getItems().stream().map(WatchlistItem::getTicker).toList();
        Map<String, MarketSnapshot> latest = snapshots.findLatestForTickers(tickers).stream()
                .collect(Collectors.toMap(MarketSnapshot::getTicker, snapshot -> snapshot));
        return watchlist.getItems().stream().map(item -> marketResponse(item, latest.get(item.getTicker()), priceChange(item.getTicker(), latest.get(item.getTicker())))).toList();
    }

    @Transactional(readOnly = true)
    public List<WatchlistDtos.InstrumentResponse> searchInstruments(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        return instruments.search(query.trim()).stream()
                .map(item -> new WatchlistDtos.InstrumentResponse(item.getTicker(), item.getCompanyName(), item.getExchange())).toList();
    }

    @Transactional
    public WatchlistResponse markReviewed(String email, Long id) {
        Watchlist watchlist = findOwned(email, id);
        watchlist.markReviewed();
        return response(watchlist);
    }

    @Transactional
    public WatchlistResponse removeItem(String email, Long id, String ticker) {
        Watchlist watchlist = findOwned(email, id);
        String normalizedTicker = cleanTicker(ticker);
        items.deleteByWatchlistIdAndTicker(id, normalizedTicker);
        watchlist.getItems().removeIf(item -> item.getTicker().equals(normalizedTicker));
        watchlist.touch();
        return response(watchlist);
    }

    private Watchlist findOwned(String email, Long id) {
        return watchlists.findByIdAndUserEmail(id, email).orElseThrow(WatchlistNotFoundException::new);
    }

    private WatchlistResponse response(Watchlist watchlist) {
        return new WatchlistResponse(watchlist.getId(), watchlist.getName(),
                watchlist.getItems().stream().map(item -> new WatchlistItemResponse(item.getTicker(), item.getCompanyName(), item.getExchange())).toList(),
                watchlist.getCreatedAt(), watchlist.getUpdatedAt(), watchlist.getReviewedAt());
    }

    private BigDecimal priceChange(String ticker, MarketSnapshot latest) {
        if (latest == null) return null;
        List<MarketSnapshot> history = snapshots.findTop2ByTickerOrderByTimestampDesc(ticker);
        if (history.size() < 2 || history.get(1).getPrice().signum() == 0) return null;
        return latest.getPrice().subtract(history.get(1).getPrice()).divide(history.get(1).getPrice(), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private WatchlistDtos.MarketDataResponse marketResponse(WatchlistItem item, MarketSnapshot snapshot, BigDecimal changePercent) {
        if (snapshot == null) return new WatchlistDtos.MarketDataResponse(item.getTicker(), item.getCompanyName(), item.getExchange(), null, null, "UNAVAILABLE", 0, "No market snapshot available", null);
        int materiality = Math.min(100, (int) Math.round(Math.abs(snapshot.getSectorChange().doubleValue()) * 10 + snapshot.getVolatility().doubleValue() * 5));
        String status = materiality >= 70 ? "MEANINGFUL" : materiality >= 35 ? "WATCHING" : "NORMAL";
        String narrative = snapshot.getSectorChange().signum() >= 0 ? "Positive sector momentum" : "Sector movement turned lower";
        return new WatchlistDtos.MarketDataResponse(item.getTicker(), item.getCompanyName(), item.getExchange(), snapshot.getPrice(), changePercent, status, materiality, narrative, snapshot.getTimestamp());
    }

    private String cleanName(String name) { return name.trim(); }
    private String cleanTicker(String ticker) { return ticker.trim().toUpperCase(); }
}