package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsArticleService {
    private final NewsArticleRepository articles;
    private final MarketService market;

    public NewsArticleService(NewsArticleRepository articles, MarketService market) {
        this.articles = articles;
        this.market = market;
    }

    @Transactional(readOnly = true)
    public List<NewsArticleDtos.ArticleResponse> byTicker(String ticker, Instant from, Instant to, int limit) {
        String normalized = market.normalizeTicker(ticker);
        if (from == null || to == null) throw new IllegalArgumentException("from and to are required.");
        if (from.isAfter(to)) throw new IllegalArgumentException("from must be before or equal to to.");
        return articles.findByTickerAndPublishedAtBetweenOrderByPublishedAtAsc(normalized, from, to, PageRequest.of(0, limit))
                .stream().map(this::response).toList();
    }

    private NewsArticleDtos.ArticleResponse response(NewsArticle article) {
        return new NewsArticleDtos.ArticleResponse(article.getId(), article.getTicker(), article.getHeadline(),
                article.getSource(), article.getPublishedAt(), article.getIngestionTimestamp(), article.getEventType(),
                article.getSentimentScore());
    }
}