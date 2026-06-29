package com.mikeline.gamedealwatcher.telegram;

import com.mikeline.gamedealwatcher.config.TelegramProperties;
import com.mikeline.gamedealwatcher.exception.WatchlistUserNotFoundException;
import com.mikeline.gamedealwatcher.model.GameSubscription;
import com.mikeline.gamedealwatcher.model.IdentityProvider;
import com.mikeline.gamedealwatcher.model.TrackedGame;
import com.mikeline.gamedealwatcher.model.WatchlistUser;
import com.mikeline.gamedealwatcher.service.GameSubscriptionService;
import com.mikeline.gamedealwatcher.service.TrackedGameService;
import com.mikeline.gamedealwatcher.service.WatchlistUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SteamDealBotTest {

    @Mock TelegramProperties properties;
    @Mock TelegramNotifier notifier;
    @Mock TrackedGameService trackedGameService;
    @Mock GameSubscriptionService gameSubscriptionService;
    @Mock WatchlistUserService watchlistUserService;

    @Mock Update update;
    @Mock Message message;
    @Mock User from;

    SteamDealBot bot;
    WatchlistUser user;

    @BeforeEach
    void setUp() {
        bot = new SteamDealBot(properties, notifier, trackedGameService, gameSubscriptionService, watchlistUserService);
        user = new WatchlistUser(IdentityProvider.TELEGRAM, "123", "testuser");

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getChatId()).thenReturn(123L);
        when(message.getFrom()).thenReturn(from);
        when(from.getUserName()).thenReturn("testuser");
    }

    // --- consume() guards ---

    @Test
    void consume_skipsUpdateWithNoMessage() {
        when(update.hasMessage()).thenReturn(false);

        bot.consume(update);

        verifyNoInteractions(watchlistUserService, notifier);
    }

    @Test
    void consume_skipsMessageWithNoText() {
        when(message.hasText()).thenReturn(false);

        bot.consume(update);

        verifyNoInteractions(watchlistUserService, notifier);
    }

    @Test
    void consume_registersNewUserOnFirstInteraction() {
        when(message.getText()).thenReturn("/help");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123"))
                .thenThrow(new WatchlistUserNotFoundException(IdentityProvider.TELEGRAM, "123"));
        when(watchlistUserService.registerIfNew(IdentityProvider.TELEGRAM, "123", "testuser"))
                .thenReturn(user);

        bot.consume(update);

        verify(watchlistUserService).registerIfNew(IdentityProvider.TELEGRAM, "123", "testuser");
        verify(notifier).sendTo(eq(123L), anyString());
    }

    @Test
    void consume_returnsEarlyAndSkipsCommandWhenUserLookupThrowsUnexpectedException() {
        when(message.getText()).thenReturn("/list");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123"))
                .thenThrow(new RuntimeException("db is down"));

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("Something went wrong"));
        verify(gameSubscriptionService, never()).findUserGameSubscriptions(any());
    }

    // --- /help ---

    @Test
    void handleHelp_sendsHelpText() {
        when(message.getText()).thenReturn("/help");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("Steam Deal Notifier"));
    }

    // --- /list ---

    @Test
    void handleList_emptyWatchlist_sendsEmptyMessage() {
        when(message.getText()).thenReturn("/list");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(gameSubscriptionService.findUserGameSubscriptions(any())).thenReturn(List.of());

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("empty"));
    }

    @Test
    void handleList_withGames_sendsGameNames() {
        TrackedGame game = new TrackedGame(730L, "CS2");
        GameSubscription sub = new GameSubscription(user, game, false);

        when(message.getText()).thenReturn("/list");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(gameSubscriptionService.findUserGameSubscriptions(any())).thenReturn(List.of(sub));

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("CS2"));
    }

    // --- /add ---

    @Test
    void handleAdd_noArg_sendsUsageHint() {
        when(message.getText()).thenReturn("/add");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("Usage"));
    }

    @Test
    void handleAdd_nonNumericAppId_sendsError() {
        when(message.getText()).thenReturn("/add notanumber");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("must be a number"));
    }

    @Test
    void handleAdd_gameAlreadyInWatchlist_sendsAlreadyAddedMessage() {
        TrackedGame game = new TrackedGame(730L, "CS2");
        when(message.getText()).thenReturn("/add 730 CS2");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(trackedGameService.existsBySteamAppId(730L)).thenReturn(true);
        when(trackedGameService.findBySteamAppId(730L)).thenReturn(game);
        when(gameSubscriptionService.isUserSubscribedToGame(any(), eq(730L))).thenReturn(true);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("already in your watchlist"));
        verify(gameSubscriptionService, never()).save(eq(user), any());
    }

    @Test
    void handleAdd_newGameNewSubscription_addsAndConfirms() {
        TrackedGame game = new TrackedGame(730L, "CS2");
        when(message.getText()).thenReturn("/add 730 CS2");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(trackedGameService.existsBySteamAppId(730L)).thenReturn(false);
        when(trackedGameService.add(any())).thenReturn(game);
        when(gameSubscriptionService.isUserSubscribedToGame(any(), eq(730L))).thenReturn(false);

        bot.consume(update);

        verify(gameSubscriptionService).save(user, game);
        verify(notifier).sendTo(eq(123L), contains("Added"));
    }

    @Test
    void handleAdd_existingGameNewSubscription_addsAndConfirms() {
        TrackedGame game = new TrackedGame(730L, "CS2");
        when(message.getText()).thenReturn("/add 730 CS2");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(trackedGameService.existsBySteamAppId(730L)).thenReturn(true);
        when(trackedGameService.findBySteamAppId(730L)).thenReturn(game);
        when(gameSubscriptionService.isUserSubscribedToGame(any(), eq(730L))).thenReturn(false);

        bot.consume(update);

        verify(gameSubscriptionService).save(user, game);
        verify(notifier).sendTo(eq(123L), contains("Added"));
    }

    // --- /remove ---

    @Test
    void handleRemove_noArg_sendsUsageHint() {
        when(message.getText()).thenReturn("/remove");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("Usage"));
    }

    @Test
    void handleRemove_nonNumericAppId_sendsError() {
        when(message.getText()).thenReturn("/remove notanumber");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("must be a number"));
    }

    @Test
    void handleRemove_gameNotInWatchlist_sendsNotFoundMessage() {
        when(message.getText()).thenReturn("/remove 730");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(gameSubscriptionService.isUserSubscribedToGame(any(), eq(730L))).thenReturn(false);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("not in your watchlist"));
        verify(gameSubscriptionService, never()).deleteGameSubscription(any(), any());
    }

    @Test
    void handleRemove_gameInWatchlist_deletesAndConfirms() {
        when(message.getText()).thenReturn("/remove 730");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);
        when(gameSubscriptionService.isUserSubscribedToGame(any(), eq(730L))).thenReturn(true);

        bot.consume(update);

        verify(gameSubscriptionService).deleteGameSubscription(any(), eq(730L));
        verify(notifier).sendTo(eq(123L), contains("Removed"));
    }

    // --- unknown command ---

    @Test
    void handleCommand_unknownCommand_sendsHelpHint() {
        when(message.getText()).thenReturn("/foobar");
        when(watchlistUserService.require(IdentityProvider.TELEGRAM, "123")).thenReturn(user);

        bot.consume(update);

        verify(notifier).sendTo(eq(123L), contains("Unknown command"));
    }
}
