package com.mikeline.gamedealwatcher.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SteamPriceDtos {
    private SteamPriceDtos() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AppDetailsWrapper(
            boolean success,
            AppData data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AppData(
            @JsonProperty("price_overview") PriceOverview priceOverview
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceOverview(
        String currency,
        int initial,
        @JsonProperty("final") int finalPrice,
        @JsonProperty("discount_percent") int discountPercent
    ) {}
}
