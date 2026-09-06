package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    boolean existsByTickerAndPublishedAtAndHeadlineAndSource(String ticker, Instant publishedAt,
            String headline, String source);

    List<NewsArticle> findByTickerAndPublishedAtBetweenOrderByPublishedAtAsc(String ticker,
            Instant from, Instant to, Pageable pageable);
}