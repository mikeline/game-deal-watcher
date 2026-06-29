package com.mikeline.gamedealwatcher.kafka;

import com.mikeline.gamedealwatcher.config.KafkaConfig;
import com.mikeline.gamedealwatcher.model.GameSubscription;
import com.mikeline.gamedealwatcher.service.GameSubscriptionService;
import com.mikeline.gamedealwatcher.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final TelegramNotifier telegramNotifier;
    private final GameSubscriptionService gameSubscriptionService;

    @KafkaListener(topics = KafkaConfig.PRICE_DROP_TOPIC, groupId = "steam-notifier")
    public void onPriceDrop(PriceDropEvent event) {
        double oldPrice = event.previousPrice() / 100.0;
        double newPrice = event.newPrice() / 100.0;

        String message = String.format(
                "🎮 <b>Price Drop!</b>%n%n" +
                        "<b>%s</b>%n" +
                        "%.2f %s → <b>%.2f %s</b>%n" +
                        "%d%% off",
                event.appName(),
                oldPrice, event.currency(),
                newPrice, event.currency(),
                event.discountPercent());

        List<GameSubscription> subscriptions = gameSubscriptionService.findUnnotifiedUsersSubscribedToGame(event.appId());

        for (GameSubscription subscription : subscriptions) {
            boolean delivered = telegramNotifier.sendTo(Long.parseLong(subscription.getUser().getExtId()), message);
            if (delivered) {
                subscription.setSaleNotificationSent(true);
                gameSubscriptionService.save(subscription);
            }
        }

        log.info("Sent price drop alert for appId {} to Telegram", event.appId());
    }
}
