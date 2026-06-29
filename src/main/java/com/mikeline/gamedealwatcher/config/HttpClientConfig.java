package com.mikeline.gamedealwatcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient steamRestClient() {
        return RestClient.builder()
                .baseUrl("https://store.steampowered.com")
                .build();
    }

}
