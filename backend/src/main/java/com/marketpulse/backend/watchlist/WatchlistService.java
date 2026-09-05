package com.marketpulse.backend.watchlist;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;
import com.marketpulse.backend.watchlist.WatchlistDtos.AddWatchlistItemRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.CreateWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.RenameWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.WatchlistItemResponse;
import com.marketpulse.backend.watchlist.WatchlistDtos.WatchlistResponse;

@Service
public class WatchlistService {
    private final WatchlistRepository watchlists;
    private final WatchlistItemRepository items;
    private final UserRepository users;

    public WatchlistService(WatchlistRepository watchlists, WatchlistItemRepository items, UserRepository users) {
        this.watchlists = watchlists;
        this.items = items;
        this.users = users;
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
        watchlist.getItems().add(items.save(new WatchlistItem(watchlist, ticker)));
        watchlist.touch();
        return response(watchlist);
    }

    @Transactional
    public WatchlistResponse removeItem(String email, Long id, String ticker) {
        Watchlist watchlist = findOwned(email, id);
        String normalizedTicker = cleanTicker(ticker);
        watchlist.getItems().removeIf(item -> item.getTicker().equals(normalizedTicker));
        watchlist.touch();
        return response(watchlist);
    }

    private Watchlist findOwned(String email, Long id) {
        return watchlists.findByIdAndUserEmail(id, email).orElseThrow(WatchlistNotFoundException::new);
    }

    private WatchlistResponse response(Watchlist watchlist) {
        return new WatchlistResponse(watchlist.getId(), watchlist.getName(),
                watchlist.getItems().stream().map(item -> new WatchlistItemResponse(item.getTicker())).toList(),
                watchlist.getCreatedAt(), watchlist.getUpdatedAt());
    }

    private String cleanName(String name) { return name.trim(); }
    private String cleanTicker(String ticker) { return ticker.trim().toUpperCase(); }
}