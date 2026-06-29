package com.mikeline.gamedealwatcher.kafka;

import com.mikeline.gamedealwatcher.model.GameSubscription;
import com.mikeline.gamedealwatcher.model.IdentityProvider;
import com.mikeline.gamedealwatcher.model.TrackedGame;
import com.mikeline.gamedealwatcher.model.WatchlistUser;
import com.mikeline.gamedealwatcher.service.GameSubscriptionService;
import com.mikeline.gamedealwatcher.telegram.TelegramNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock TelegramNotifier telegramNotifier;
    @Mock GameSubscriptionService gameSubscriptionService;

    NotificationConsumer consumer;

    PriceDropEvent event;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(telegramNotifier, gameSubscriptionService);
        event = new PriceDropEvent(730L, "Counter-Strike 2", "USD", 2999, 1499, 50, Instant.now());
    }

    @Test
    void onPriceDrop_noSubscribers_sendsNothing() {
        when(gameSubscriptionService.findUnnotifiedUsersSubscribedToGame(730L)).thenReturn(List.of());

        consumer.onPriceDrop(event);

        verify(telegramNotifier, never()).sendTo(any(), anyString());
        verify(gameSubscriptionService, never()).save(any(GameSubscription.class));
    }

    @Test
    void onPriceDrop_deliverySucceeds_marksSubscriptionAsSentAndSaves() {
        GameSubscription sub = makeSubscription("123");
        when(gameSubscriptionService.findUnnotifiedUsersSubscribedToGame(730L)).thenReturn(List.of(sub));
        when(telegramNotifier.sendTo(eq(123L), anyString())).thenReturn(true);

        consumer.onPriceDrop(event);

        assertThat(sub.getSaleNotificationSent()).isTrue();
        verify(gameSubscriptionService).save(sub);
    }

    @Test
    void onPriceDrop_deliveryFails_doesNotMarkAsSentOrSave() {
        GameSubscription sub = makeSubscription("123");
        when(gameSubscriptionService.findUnnotifiedUsersSubscribedToGame(730L)).thenReturn(List.of(sub));
        when(telegramNotifier.sendTo(eq(123L), anyString())).thenReturn(false);

        consumer.onPriceDrop(event);

        assertThat(sub.getSaleNotificationSent()).isFalse();
        verify(gameSubscriptionService, never()).save(any(GameSubscription.class));
    }

    @Test
    void onPriceDrop_partialDeliveryFailure_onlyMarksSuccessfulDeliveries() {
        GameSubscription sub1 = makeSubscription("111");
        GameSubscription sub2 = makeSubscription("222");
        when(gameSubscriptionService.findUnnotifiedUsersSubscribedToGame(730L)).thenReturn(List.of(sub1, sub2));
        when(telegramNotifier.sendTo(eq(111L), anyString())).thenReturn(true);
        when(telegramNotifier.sendTo(eq(222L), anyString())).thenReturn(false);

        consumer.onPriceDrop(event);

        assertThat(sub1.getSaleNotificationSent()).isTrue();
        assertThat(sub2.getSaleNotificationSent()).isFalse();
        verify(gameSubscriptionService).save(sub1);
        verify(gameSubscriptionService, never()).save(sub2);
    }

    @Test
    void onPriceDrop_messageContainsGameNamePricesAndDiscount() {
        GameSubscription sub = makeSubscription("123");
        when(gameSubscriptionService.findUnnotifiedUsersSubscribedToGame(730L)).thenReturn(List.of(sub));
        when(telegramNotifier.sendTo(eq(123L), anyString())).thenReturn(true);

        consumer.onPriceDrop(event);

        verify(telegramNotifier).sendTo(eq(123L), argThat(msg ->
                msg.contains("Counter-Strike 2")
                && msg.contains("29.99")
                && msg.contains("14.99")
                && msg.contains("50%")));
    }

    // --- helpers ---

    private GameSubscription makeSubscription(String chatId) {
        WatchlistUser user = new WatchlistUser(IdentityProvider.TELEGRAM, chatId, "user_" + chatId);
        TrackedGame game = new TrackedGame(730L, "Counter-Strike 2");
        return new GameSubscription(user, game, false);
    }

}
