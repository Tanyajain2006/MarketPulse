package com.marketpulse.backend.watchlist;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshot, Long> {
    boolean existsByTickerAndTimestamp(String ticker, java.time.Instant timestamp);

    List<MarketSnapshot> findTop2ByTickerOrderByTimestampDesc(String ticker);
    @Query("select s from MarketSnapshot s where s.ticker in :tickers and s.timestamp = (select max(latest.timestamp) from MarketSnapshot latest where latest.ticker = s.ticker)")
    List<MarketSnapshot> findLatestForTickers(@Param("tickers") List<String> tickers);
}