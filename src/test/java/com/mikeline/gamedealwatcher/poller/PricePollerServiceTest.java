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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Note: pollPrices() sleeps 1500 ms per game due to rate-limiting.
// Each test below runs with a single tracked game and therefore adds ~1.5 s to its wall-clock time.
@ExtendWith(MockitoExtension.class)
class PricePollerServiceTest {

    @Mock TrackedGameRepository trackedGameRepository;
    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock SteamApiClient steamApiClient;
    @Mock PriceEventProducer priceEventProducer;
    @Mock GameSubscriptionService gameSubscriptionService;

    PricePollerService poller;

    TrackedGame game;

    @BeforeEach
    void setUp() {
        poller = new PricePollerService(
                trackedGameRepository, priceHistoryRepository,
                steamApiClient, priceEventProducer, gameSubscriptionService);
        game = new TrackedGame(730L, "CS2");
    }

    @Test
    void pollPrices_noTrackedGames_doesNothing() {
        when(trackedGameRepository.findAll()).thenReturn(List.of());

        poller.pollPrices();

        verify(steamApiClient, never()).fetchPrice(any());
    }

    @Test
    void pollSingle_steamApiReturnsEmpty_skipsGame() throws Exception {
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.empty());

        poller.pollPrices();

        verify(priceHistoryRepository, never()).save(any());
        verify(priceEventProducer, never()).publish(any());
    }

    @Test
    void pollSingle_firstObservation_savesBaselineAndDoesNotPublishEvent() throws Exception {
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(2999, 2999, 0)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.empty());

        poller.pollPrices();

        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(priceEventProducer, never()).publish(any());
    }

    @Test
    void pollSingle_firstObservationWhileOnSale_savesBaselineAndDoesNotPublishEvent() throws Exception {
        // Regression: before the fix a game already on sale triggered a spurious price-drop event
        // on its first poll because previousPrice fell back to current.initial().
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(2999, 1499, 50)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.empty());

        poller.pollPrices();

        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(priceEventProducer, never()).publish(any());
    }

    @Test
    void pollSingle_priceUnchanged_doesNotSaveOrPublish() throws Exception {
        PriceHistory last = new PriceHistory(730L, "USD", 2999, 0);
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(2999, 2999, 0)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.of(last));
        when(gameSubscriptionService.countUnnotifiedSubscribers(730L)).thenReturn(0);

        poller.pollPrices();

        verify(priceHistoryRepository, never()).save(any());
        verify(priceEventProducer, never()).publish(any());
    }

    @Test
    void pollSingle_priceDrop_withUnnotifiedSubscribers_savesAndPublishesEvent() throws Exception {
        PriceHistory last = new PriceHistory(730L, "USD", 2999, 0);
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(2999, 1499, 50)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.of(last));
        when(gameSubscriptionService.countUnnotifiedSubscribers(730L)).thenReturn(3);

        poller.pollPrices();

        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(priceEventProducer).publish(any(PriceDropEvent.class));
    }

    @Test
    void pollSingle_priceDrop_allSubscribersAlreadyNotified_savesButDoesNotPublish() throws Exception {
        PriceHistory last = new PriceHistory(730L, "USD", 2999, 0);
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(2999, 1499, 50)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.of(last));
        when(gameSubscriptionService.countUnnotifiedSubscribers(730L)).thenReturn(0);

        poller.pollPrices();

        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(priceEventProducer, never()).publish(any());
    }

    @Test
    void pollSingle_basePriceIncrease_resetsNotificationStatusWithoutPublishing() throws Exception {
        PriceHistory last = new PriceHistory(730L, "USD", 2999, 0);
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(3999, 3999, 0)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.of(last));
        when(gameSubscriptionService.countUnnotifiedSubscribers(730L)).thenReturn(0);

        poller.pollPrices();

        verify(gameSubscriptionService).resetNotificationStatus(730L);
        verify(priceEventProducer, never()).publish(any());
    }

    @Test
    void pollSingle_promotionEnded_resetsNotificationStatusWithoutPublishing() throws Exception {
        PriceHistory last = new PriceHistory(730L, "USD", 1499, 50);
        when(trackedGameRepository.findAll()).thenReturn(List.of(game));
        when(steamApiClient.fetchPrice(730L)).thenReturn(Optional.of(price(2999, 2999, 0)));
        when(priceHistoryRepository.findTopBySteamAppIdOrderByRecordedAtDesc(730L)).thenReturn(Optional.of(last));
        when(gameSubscriptionService.countUnnotifiedSubscribers(730L)).thenReturn(0);

        poller.pollPrices();

        verify(gameSubscriptionService).resetNotificationStatus(730L);
        verify(priceEventProducer, never()).publish(any());
    }

    // --- helpers ---

    private SteamPriceDtos.PriceOverview price(int initial, int finalPrice, int discountPercent) {
        return new SteamPriceDtos.PriceOverview("USD", initial, finalPrice, discountPercent);
    }
}
