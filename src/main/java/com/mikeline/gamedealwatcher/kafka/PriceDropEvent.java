package com.mikeline.gamedealwatcher.kafka;

import java.time.Instant;

public record PriceDropEvent(
        Long appId,
        String appName,
        String currency,
        int previousPrice,
        int newPrice,
        int discountPercent,
        Instant detectedAt
) {
}
