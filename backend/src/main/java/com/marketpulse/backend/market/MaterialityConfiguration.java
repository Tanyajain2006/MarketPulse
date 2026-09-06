package com.marketpulse.backend.market;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaterialityConfiguration {
    @Bean
    MaterialityScoreEngine materialityScoreEngine(
            @Value("${marketpulse.materiality.price-threshold:5}") BigDecimal priceThreshold,
            @Value("${marketpulse.materiality.sector-threshold:2}") BigDecimal sectorThreshold,
            @Value("${marketpulse.materiality.price-weight:30}") BigDecimal priceWeight,
            @Value("${marketpulse.materiality.volume-weight:25}") BigDecimal volumeWeight,
            @Value("${marketpulse.materiality.volatility-weight:20}") BigDecimal volatilityWeight,
            @Value("${marketpulse.materiality.sector-weight:15}") BigDecimal sectorWeight,
            @Value("${marketpulse.materiality.peer-weight:10}") BigDecimal peerWeight) {
        return new MaterialityScoreEngine(priceThreshold, sectorThreshold, priceWeight, volumeWeight,
                volatilityWeight, sectorWeight, peerWeight);
    }
}