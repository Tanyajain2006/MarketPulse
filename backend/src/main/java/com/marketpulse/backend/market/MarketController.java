package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketService market;
    private final MarketDataImportService importer;
    private final MarketChangeService changes;

    public MarketController(MarketService market, MarketDataImportService importer, MarketChangeService changes) {
        this.market = market;
        this.importer = importer;
        this.changes = changes;
    }

    @PostMapping("/history/import")
    public MarketDataImportService.ImportSummary importHistory() {
        return importer.importConfiguredFile();
    }

    @GetMapping("/{ticker}/latest")
    public MarketSnapshotDtos.SnapshotResponse latest(@PathVariable String ticker) {
        return market.latest(ticker);
    }

    @GetMapping("/{ticker}/changes")
    public MarketChangeDtos.ChangeResponse changes(@PathVariable String ticker) {
        return changes.changes(ticker);
    }

    @GetMapping("/{ticker}/history")
    public List<MarketSnapshotDtos.SnapshotResponse> history(@PathVariable String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {
        return market.history(ticker, from, to, Math.min(Math.max(limit, 1), 1000));
    }
}
