package com.mikeline.gamedealwatcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramProperties (String token,
                                  Long allowedChatId) {
}
