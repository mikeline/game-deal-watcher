package com.mikeline.gamedealwatcher.dto;

import com.mikeline.gamedealwatcher.model.PriceHistory;

import java.time.Instant;

public record PriceHistoryResponse(Long id,
                                   Long appId,
                                   String currency,
                                   int finalPrice,
                                   int discountPercent,
                                   Instant recordedAt) {
    public static PriceHistoryResponse from(PriceHistory priceHistory) {
        return new PriceHistoryResponse(priceHistory.getId(),
                                        priceHistory.getSteamAppId(),
                                        priceHistory.getCurrency(),
                                        priceHistory.getFinalPrice(),
                                        priceHistory.getDiscountPercent(),
                                        priceHistory.getRecordedAt());
    }
}
