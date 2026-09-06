package com.marketpulse.backend.watchlist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {
    Optional<Instrument> findByTickerIgnoreCase(String ticker);

    @Query("select i from Instrument i where lower(i.ticker) like lower(concat('%', :query, '%')) or lower(i.companyName) like lower(concat('%', :query, '%')) order by i.ticker")
    List<Instrument> search(@Param("query") String query);
}