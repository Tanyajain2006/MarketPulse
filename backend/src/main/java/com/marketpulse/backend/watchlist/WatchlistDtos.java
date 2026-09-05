package com.marketpulse.backend.watchlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class WatchlistDtos {
    private WatchlistDtos() { }

    public record CreateWatchlistRequest(
            @NotBlank @Size(max = 100) String name) { }

    public record RenameWatchlistRequest(
            @NotBlank @Size(max = 100) String name) { }

    public record AddWatchlistItemRequest(
            @NotBlank @Size(max = 15)
            @Pattern(regexp = "[A-Za-z0-9.-]+", message = "must be a valid ticker") String ticker) { }

    public record WatchlistItemResponse(String ticker) { }

    public record WatchlistResponse(
            Long id, String name, List<WatchlistItemResponse> items,
            Instant createdAt, Instant updatedAt) { }
}