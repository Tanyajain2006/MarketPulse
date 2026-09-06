package com.marketpulse.backend.market;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsEventRepository extends JpaRepository<NewsEvent, Long> {
    boolean existsByTickerAndTimestampAndHeadline(String ticker, Instant timestamp, String headline);

    List<NewsEvent> findByTickerAndTimestampBetweenOrderByTimestampDesc(String ticker, Instant from, Instant to,
            Pageable pageable);

    List<NewsEvent> findByTickerOrderByTimestampDesc(String ticker, Pageable pageable);

    @Query("select n from NewsEvent n where n.ticker in :tickers and n.timestamp >= :from order by n.timestamp desc")
    List<NewsEvent> findLatestForTickers(@Param("tickers") List<String> tickers, @Param("from") Instant from,
            Pageable pageable);

    @Query("select n from NewsEvent n where n.ticker = :ticker and n.timestamp between :from and :to and (:eventType is null or n.eventType = :eventType) order by n.timestamp desc")
    List<NewsEvent> findFiltered(@Param("ticker") String ticker, @Param("from") Instant from,
            @Param("to") Instant to, @Param("eventType") String eventType, Pageable pageable);

    @Query("select n from NewsEvent n where n.timestamp >= :from and n.ticker in :tickers order by n.timestamp desc")
    List<NewsEvent> findSinceForTickers(@Param("tickers") List<String> tickers, @Param("from") Instant from);
}
