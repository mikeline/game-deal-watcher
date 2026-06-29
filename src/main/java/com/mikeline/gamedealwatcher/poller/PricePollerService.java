package com.mikeline.gamedealwatcher.poller;

import com.mikeline.gamedealwatcher.kafka.PriceDropEvent;
import com.mikeline.gamedealwatcher.kafka.PriceEventProducer;
import com.mikeline.gamedealwatcher.model.PriceHistory;
import com.mikeline.gamedealwatcher.model.TrackedGame;
import com.mikeline.gamedealwatcher.repository.PriceHistoryRepository;
import com.mikeline.gamedealwatcher.repository.TrackedGameRepository;
import com.mikeline.gamedealwatcher.service.GameSubscriptionService;
import com.mikeline.gamedealwatcher.steam.SteamApiClient;
import com.mikeline.gamedealwatcher.steam.SteamPriceDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricePollerService {
    private final TrackedGameRepository trackedGameRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final SteamApiClient steamApiClient;
    private final PriceEventProducer priceEventProducer;
    private final GameSubscriptionService gameSubscriptionService;

    @Scheduled(fixedDelayString = "${app.poll.interval-ms:1800000}")
    public void pollPrices() {
        List<TrackedGame> items = trackedGameRepository.findAll();
        log.info("Starting price poll for {} games", items.size());

        for (TrackedGame item : items) {
            try {
                pollSingle(item);
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Poll interrupted, stopping cycle");
                return;
            } catch (Exception e) {
                log.error("Error polling appId {}: {}", item.getSteamAppId(), e.getMessage());
            }
        }
        log.info("Price poll complete");
    }

    private void pollSingle(TrackedGame item) {
        Long appId = item.getSteamAppId();
        Optional<SteamPriceDtos.PriceOverview> maybePrice = steamApiClient.fetchPrice(appId);

        if (maybePrice.isEmpty()) {
            return; // free game, delisted, or transient error - skip
        }

        SteamPriceDtos.PriceOverview current = maybePrice.get();

        Optional<PriceHistory> lastRecord = priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(appId);
        PriceHistory record = new PriceHistory(appId, current.currency(),
                current.finalPrice(), current.discountPercent());

        if (lastRecord.isEmpty()) {
            log.info("First price recorded for '{}' (appId {}): {} {}",
                    item.getAppName(), appId, current.finalPrice() / 100, current.currency());
            priceHistoryRepository.save(record);
            return;
        }

        int previousPrice = lastRecord.get().getFinalPrice();
        int currentPrice = current.finalPrice();

        if (currentPrice != previousPrice) {
            priceHistoryRepository.save(record);
        }

        int countUnnotifiedSubscribers = gameSubscriptionService.countUnnotifiedSubscribers(appId);

        if (countUnnotifiedSubscribers > 0 && (currentPrice < previousPrice || current.discountPercent() > 0)) {
            PriceDropEvent event = new PriceDropEvent(
                    appId, item.getAppName(), current.currency(),
                    previousPrice, currentPrice, current.discountPercent(), Instant.now()
            );
            priceEventProducer.publish(event);
        } else if (currentPrice > previousPrice && lastRecord.map(PriceHistory::getDiscountPercent).orElse(0) == 0) {
            int increaseCents = currentPrice - previousPrice;
            gameSubscriptionService.resetNotificationStatus(appId);
            log.info("BASE PRICE INCREASED '{}' (appId {}): {} -> {} {} (+{})",
                    item.getAppName(), appId,
                    previousPrice / 100, currentPrice / 100, current.currency(),
                    increaseCents);
            //TODO: Need to track it somehow to show more realistic discount later
        } else if (currentPrice > previousPrice) {
            int increaseCents = currentPrice - previousPrice;
            gameSubscriptionService.resetNotificationStatus(appId);
            log.info("PROMOTION RESET '{}' (appId {}): {} -> {} {} (+{})",
                    item.getAppName(), appId,
                    previousPrice / 100, currentPrice / 100, current.currency(),
                    increaseCents);
        } else {
            log.debug("No drop for appId {} ({} -> {})", appId, previousPrice, currentPrice);
        }
    }
}
