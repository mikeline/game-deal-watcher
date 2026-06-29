package com.mikeline.gamedealwatcher.service;

import com.mikeline.gamedealwatcher.model.GameSubscription;
import com.mikeline.gamedealwatcher.model.TrackedGame;
import com.mikeline.gamedealwatcher.model.WatchlistUser;
import com.mikeline.gamedealwatcher.repository.GameSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameSubscriptionService {

    private final GameSubscriptionRepository gameSubscriptionRepository;

    @Transactional(readOnly = true)
    public List<GameSubscription> findUserGameSubscriptions(Long userId) {
        return gameSubscriptionRepository.findByUserIdWithGame(userId);
    }

    @Transactional(readOnly = true)
    public boolean isUserSubscribedToGame(Long userId, Long appId) {
        return gameSubscriptionRepository.existsByUser_UserIdAndGame_SteamAppId(userId, appId);
    }

    @Transactional
    public void deleteGameSubscription(Long userId, Long appId) {
        gameSubscriptionRepository.deleteByUser_UserIdAndGame_SteamAppId(userId, appId);
    }

    @Transactional(readOnly = true)
    public List<GameSubscription> findUsersSubscribedToGame(Long appId) {
        return gameSubscriptionRepository.findByAppIdWithUser(appId);
    }

    @Transactional(readOnly = true)
    public List<GameSubscription> findUnnotifiedUsersSubscribedToGame(Long appId) {
        return gameSubscriptionRepository.findByAppIdWithUserUnnotified(appId);
    }


    @Transactional
    public GameSubscription save(WatchlistUser user, TrackedGame game) {
        return gameSubscriptionRepository.save(new GameSubscription(user, game, false));
    }

    @Transactional
    public GameSubscription save(GameSubscription gameSubscription) {
        return gameSubscriptionRepository.save(gameSubscription);
    }

    @Transactional(readOnly = true)
    public Integer countUnnotifiedSubscribers(Long appId) {
        return gameSubscriptionRepository.countByGame_SteamAppIdAndSaleNotificationSent(appId, false);
    }

    @Transactional
    public void resetNotificationStatus(Long appId) {
        gameSubscriptionRepository.resetNotificationSent(appId);
    }
}
