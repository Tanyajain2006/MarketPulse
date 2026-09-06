package com.marketpulse.backend.watchlist;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshot, Long> {
    boolean existsByTickerAndObservationTimestamp(String ticker, java.time.Instant timestamp);

    List<MarketSnapshot> findTop2ByTickerOrderByObservationTimestampDesc(String ticker);
        @Query("select s from MarketSnapshot s where s.ticker = :ticker and s.observationTimestamp >= :from order by s.observationTimestamp desc")
        List<MarketSnapshot> findSinceForTicker(@Param("ticker") String ticker, @Param("from") java.time.Instant from,
            Pageable pageable);
        List<MarketSnapshot> findByTickerAndObservationTimestampBetweenOrderByObservationTimestampDesc(String ticker, java.time.Instant from,
            java.time.Instant to, Pageable pageable);
        List<MarketSnapshot> findByTickerOrderByObservationTimestampDesc(String ticker, Pageable pageable);

        @Query("select s from MarketSnapshot s where s.ticker in :tickers and s.observationTimestamp >= :from order by s.observationTimestamp desc")
        List<MarketSnapshot> findSinceForTickers(@Param("tickers") List<String> tickers, @Param("from") java.time.Instant from);
    @Query("select s from MarketSnapshot s where s.ticker in :tickers and s.observationTimestamp = (select max(latest.observationTimestamp) from MarketSnapshot latest where latest.ticker = s.ticker)")
    List<MarketSnapshot> findLatestForTickers(@Param("tickers") List<String> tickers);
}