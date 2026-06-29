package com.mikeline.gamedealwatcher.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PRICE_DROP_TOPIC = "price.drop";

    @Bean
    public NewTopic priceDropTopic() {
        return TopicBuilder.name(PRICE_DROP_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
