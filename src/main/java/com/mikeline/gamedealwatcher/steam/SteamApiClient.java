package com.mikeline.gamedealwatcher.steam;

import com.mikeline.gamedealwatcher.steam.SteamPriceDtos.AppDetailsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SteamApiClient {
    private final RestClient steamRestClient;

    public Optional<SteamPriceDtos.PriceOverview> fetchPrice(Long appId) {
        try {

            Map<String, SteamPriceDtos.AppDetailsWrapper> response = steamRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/appdetails")
                            .queryParam("appids", appId)
                            .queryParam("filters", "price_overview")
                            .queryParam("cc", "us")
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>(){});

            if (response == null) {
                log.warn("Null response from Steam for appId {}", appId);
                return Optional.empty();
            }

            AppDetailsWrapper wrapper = response.get(String.valueOf(appId));

            if (wrapper == null || !wrapper.success() || wrapper.data() == null
                  || wrapper.data().priceOverview() == null) {
                log.info("No price available for appId {} (free game or no data)", appId);
                return Optional.empty();
            }

            return Optional.of(wrapper.data().priceOverview());

        } catch (Exception ex) {
            log.error("Failed to fetch price for appId {}: {}", appId, ex.getMessage());
            return Optional.empty();
        }
    }
}
