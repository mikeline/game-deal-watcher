package com.mikeline.gamedealwatcher.telegram;

import com.mikeline.gamedealwatcher.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotifier {

    private final TelegramClient telegramClient;
    private final TelegramProperties properties;

    public void send(String text) {
        sendTo(properties.allowedChatId(), text);
    }

    public boolean sendTo(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();

        try {
            telegramClient.execute(message);
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message: {}", e.getMessage());
            return false;
        }
    }
}
