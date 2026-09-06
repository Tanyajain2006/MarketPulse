package com.marketpulse.backend.research;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.market.MarketService;
import com.marketpulse.backend.market.MarketSnapshotDtos;
import com.marketpulse.backend.market.NewsService;
import com.marketpulse.backend.watchlist.InstrumentRepository;

@Service
public class ResearchService {
    private final InstrumentRepository instruments;
    private final MarketService market;
    private final NewsService news;

    public ResearchService(InstrumentRepository instruments, MarketService market, NewsService news) {
        this.instruments = instruments;
        this.market = market;
        this.news = news;
    }

    @Transactional(readOnly = true)
    public ResearchResponse search(String query) {
        if (query == null || query.trim().length() < 2) throw new IllegalArgumentException("Query must contain at least two characters.");
        List<String> tickers = instruments.search(query.trim()).stream().limit(20).map(item -> item.getTicker()).toList();
        return new ResearchResponse(instruments.search(query.trim()).stream().limit(20)
                .map(item -> new InstrumentResult(item.getTicker(), item.getCompanyName(), item.getExchange())).toList(),
                market.latest(tickers), tickers.isEmpty() ? List.of() : news.latest(tickers, 25));
    }

    public record InstrumentResult(String ticker, String companyName, String exchange) { }
    public record ResearchResponse(List<InstrumentResult> instruments, List<MarketSnapshotDtos.SnapshotResponse> market,
            List<MarketSnapshotDtos.NewsResponse> news) { }
}
