package com.mikeline.gamedealwatcher.kafka;

import com.mikeline.gamedealwatcher.config.KafkaConfig;
import com.mikeline.gamedealwatcher.model.GameSubscription;
import com.mikeline.gamedealwatcher.service.GameSubscriptionService;
import com.mikeline.gamedealwatcher.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final TelegramNotifier telegramNotifier;
    private final GameSubscriptionService gameSubscriptionService;

    @KafkaListener(topics = KafkaConfig.PRICE_DROP_TOPIC, groupId = "steam-notifier")
    public void onPriceDrop(PriceDropEvent event) {
        BigDecimal oldPrice = BigDecimal.valueOf(event.previousPrice() / 100.0);
        BigDecimal newPrice = BigDecimal.valueOf(event.newPrice() / 100.0);
        BigDecimal lowestBasePrice = BigDecimal.valueOf(event.lowestBasePrice() / 100.0);

        // C4: lowestBasePrice==0 means no base data (old message or uninitialized row)
        // C8: only show base info when realDiscount is positive; negative means stale lowestBasePrice
        boolean hasBaseInfo = event.lowestBasePrice() > 0
                && event.lowestBasePrice() != event.previousPrice();
        double realDiscount = hasBaseInfo
                ? (double) (event.lowestBasePrice() - event.newPrice()) / event.lowestBasePrice() * 100.0
                : 0.0;
        if (realDiscount <= 0) hasBaseInfo = false;

        StringBuilder fmt = new StringBuilder("🎮 <b>Price Drop!</b>%n%n<b>%s</b>%n%.2f %s → <b>%.2f %s</b>%n");
        if (hasBaseInfo) {
            fmt.append("Initial Price: %.2f %s%n");
        }
        fmt.append("%d%% off");
        if (hasBaseInfo) {
            fmt.append("%nReal Discount: %.0f%% off");
        }

        Object[] args;
        if (hasBaseInfo) {
            args = new Object[]{event.appName(), oldPrice, event.currency(), newPrice, event.currency(),
                    lowestBasePrice, event.currency(), event.discountPercent(), realDiscount};
        } else {
            args = new Object[]{event.appName(), oldPrice, event.currency(), newPrice, event.currency(),
                    event.discountPercent()};
        }

        String message = String.format(fmt.toString(), args);

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
