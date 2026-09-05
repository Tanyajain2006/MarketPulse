package com.marketpulse.backend.watchlist;

import com.marketpulse.backend.watchlist.WatchlistDtos.AddWatchlistItemRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.CreateWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.RenameWatchlistRequest;
import com.marketpulse.backend.watchlist.WatchlistDtos.WatchlistResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistController {
    private final WatchlistService service;
    public WatchlistController(WatchlistService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistResponse create(Authentication authentication, @Valid @RequestBody CreateWatchlistRequest request) {
        return service.create(authentication.getName(), request);
    }

    @GetMapping
    public List<WatchlistResponse> list(Authentication authentication) { return service.list(authentication.getName()); }

    @GetMapping("/{id}")
    public WatchlistResponse get(Authentication authentication, @PathVariable Long id) { return service.get(authentication.getName(), id); }

    @PutMapping("/{id}")
    public WatchlistResponse rename(Authentication authentication, @PathVariable Long id, @Valid @RequestBody RenameWatchlistRequest request) {
        return service.rename(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) { service.delete(authentication.getName(), id); }

    @PostMapping("/{id}/items")
    public WatchlistResponse addItem(Authentication authentication, @PathVariable Long id, @Valid @RequestBody AddWatchlistItemRequest request) {
        return service.addItem(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}/items/{ticker}")
    public WatchlistResponse removeItem(Authentication authentication, @PathVariable Long id, @PathVariable String ticker) {
        return service.removeItem(authentication.getName(), id, ticker);
    }
}