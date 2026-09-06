package com.marketpulse.backend.watchlist;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class WatchlistDtos {
    private WatchlistDtos() { }

    public record CreateWatchlistRequest(
            @NotBlank @Size(max = 100) String name) { }

    public record RenameWatchlistRequest(
            @NotBlank @Size(max = 100) String name) { }

        public record AddWatchlistItemRequest(
            @NotBlank @Size(max = 15)
            @Pattern(regexp = "[A-Za-z0-9.-]+", message = "must be a valid ticker") String ticker) { }

    public record WatchlistItemResponse(String ticker, String companyName, String exchange) { }

    public record InstrumentResponse(String ticker, String companyName, String exchange) { }

    public record MarketDataResponse(String ticker, String companyName, String exchange,
            java.math.BigDecimal price, java.math.BigDecimal changePercent, String marketStatus,
            int materiality, String narrative, Instant timestamp) { }

    public record WatchlistResponse(
            Long id, String name, List<WatchlistItemResponse> items,
            Instant createdAt, Instant updatedAt, Instant reviewedAt) { }
}