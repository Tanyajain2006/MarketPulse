package com.marketpulse.backend.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class Instrument {
    @Id
    @Column(length = 15)
    private String ticker;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 30)
    private String exchange;

    protected Instrument() { }

    public Instrument(String ticker, String companyName, String exchange) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.exchange = exchange;
    }

    public String getTicker() { return ticker; }
    public String getCompanyName() { return companyName; }
    public String getExchange() { return exchange; }
}