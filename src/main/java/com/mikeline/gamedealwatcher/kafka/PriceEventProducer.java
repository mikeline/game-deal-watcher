package com.mikeline.gamedealwatcher.kafka;

import com.mikeline.gamedealwatcher.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceEventProducer {

    private final KafkaTemplate<String, PriceDropEvent> kafkaTemplate;

    public void publish(PriceDropEvent event) {
        kafkaTemplate.send(KafkaConfig.PRICE_DROP_TOPIC, String.valueOf(event.appId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish price drop event for appId {}: {}", event.appId(), ex.getMessage());
                    } else {
                        log.info("Published price drop event for appId {}", event.appId());
                    }
                });
    }
}
