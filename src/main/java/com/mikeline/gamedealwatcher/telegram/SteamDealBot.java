package com.mikeline.gamedealwatcher.telegram;

import com.mikeline.gamedealwatcher.config.TelegramProperties;
import com.mikeline.gamedealwatcher.dto.AddWatchlistRequest;
import com.mikeline.gamedealwatcher.exception.WatchlistUserNotFoundException;
import com.mikeline.gamedealwatcher.model.GameSubscription;
import com.mikeline.gamedealwatcher.model.IdentityProvider;
import com.mikeline.gamedealwatcher.model.TrackedGame;
import com.mikeline.gamedealwatcher.model.WatchlistUser;
import com.mikeline.gamedealwatcher.service.GameSubscriptionService;
import com.mikeline.gamedealwatcher.service.TrackedGameService;
import com.mikeline.gamedealwatcher.service.WatchlistUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SteamDealBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramProperties properties;
    private final TelegramNotifier notifier;
    private final TrackedGameService trackedGameService;
    private final GameSubscriptionService gameSubscriptionService;
    private final WatchlistUserService watchlistUserService;

    @Override
    public String getBotToken() {
        return properties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        // Register user if new
        String extId = String.valueOf(chatId);
        WatchlistUser watchlistUser = null;
        try {
            watchlistUser = watchlistUserService.require(IdentityProvider.TELEGRAM, extId);
        } catch(WatchlistUserNotFoundException e) {
            watchlistUser = watchlistUserService.registerIfNew(
                    IdentityProvider.TELEGRAM, extId, update.getMessage().getFrom().getUserName());
        } catch (Exception e) {
            log.error("Error fetching or registering user ID '{}': {}", extId, e.getMessage());
            notifier.sendTo(chatId, "⚠️ Something went wrong: " + e.getMessage());
            return;
        }

        try {
            handleCommand(chatId, text, watchlistUser);
        } catch (Exception e) {
            log.error("Error handling command '{}': {}", text, e.getMessage());
            notifier.sendTo(chatId, "⚠️ Something went wrong: " + e.getMessage());
        }
    }

    private void handleCommand(Long chatId, String text, WatchlistUser watchlistUser) {
        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : null;

        switch (command) {
            case "/start", "/help" -> sendHelp(chatId);
            case "/list" -> handleList(chatId, watchlistUser);
            case "/add" -> handleAdd(chatId, arg, watchlistUser);
            case "/remove" -> handleRemove(chatId, arg, watchlistUser);
            default -> notifier.sendTo(chatId,
                    "Unknown command. Send /help to see what I can do.");
        }
    }

    private void sendHelp(Long chatId) {
        String help = """
                <b>Steam Deal Notifier</b>

                /list — show your watchlist
                /add &lt;appId&gt; [name] — add a game
                /remove &lt;appId&gt; — remove a game
                /help — show this message

                Find an appId in a game's Steam store URL:
                store.steampowered.com/app/<b>1245620</b>/ → 1245620""";
        notifier.sendTo(chatId, help);
    }

    private void handleList(Long chatId, WatchlistUser watchlistUser) {
        List<GameSubscription> subscriptions = gameSubscriptionService.findUserGameSubscriptions(watchlistUser.getUserId());
        if (subscriptions.isEmpty()) {
            notifier.sendTo(chatId, "Your watchlist is empty. Add a game with /add &lt;appId&gt;");
            return;
        }
        StringBuilder sb = new StringBuilder("<b>Your watchlist:</b>\n\n");
        for (GameSubscription item : subscriptions) {
            TrackedGame game = item.getGame();
            sb.append("• ")
                    .append(game.getAppName() != null ? game.getAppName() : "Unknown")
                    .append(" (")
                    .append(game.getSteamAppId())
                    .append(")\n");
        }
        notifier.sendTo(chatId, sb.toString());
    }

    private void handleAdd(Long chatId, String arg, WatchlistUser watchlistUser) {
        if (arg == null || arg.isBlank()) {
            notifier.sendTo(chatId, "Usage: /add &lt;appId&gt; [name]\nExample: /add 1245620 Elden Ring");
            return;
        }
        String[] argParts = arg.split("\\s+", 2);
        Long appId;
        try {
            appId = Long.parseLong(argParts[0]);
        } catch (NumberFormatException e) {
            notifier.sendTo(chatId, "appId must be a number. Example: /add 1245620 Elden Ring");
            return;
        }
        String name = argParts.length > 1 ? argParts[1] : null;

        TrackedGame game;

        if (!trackedGameService.existsBySteamAppId(appId)) {
            game = trackedGameService.add(new AddWatchlistRequest(appId, name));
        } else {
            game = trackedGameService.findBySteamAppId(appId);
        }

        if(gameSubscriptionService.isUserSubscribedToGame(watchlistUser.getUserId(), appId)) {
            notifier.sendTo(chatId, "✅ Game " + (name != null ? name : appId) + " is already in your watchlist.");
        } else {
            gameSubscriptionService.save(watchlistUser, game);
            notifier.sendTo(chatId, "✅ Added " + (name != null ? name : appId) + " to your watchlist.");
        }
    }

    private void handleRemove(Long chatId, String arg, WatchlistUser watchlistUser) {
        if (arg == null || arg.isBlank()) {
            notifier.sendTo(chatId, "Usage: /remove &lt;appId&gt;\nExample: /remove 1245620");
            return;
        }
        Long appId;
        try {
            appId = Long.parseLong(arg.trim());
        } catch (NumberFormatException e) {
            notifier.sendTo(chatId, "appId must be a number. Example: /remove 1245620");
            return;
        }
        if (!gameSubscriptionService.isUserSubscribedToGame(watchlistUser.getUserId(), appId)) {
            notifier.sendTo(chatId, appId + " is not in your watchlist.");
            return;
        }
        gameSubscriptionService.deleteGameSubscription(watchlistUser.getUserId(), appId);
        notifier.sendTo(chatId, "🗑️ Removed " + appId + " from your watchlist.");
    }
}