package com.mikeline.gamedealwatcher.repository;

import com.mikeline.gamedealwatcher.model.GameSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GameSubscriptionRepository extends JpaRepository<GameSubscription, Long> {

    // --- Bot: a user's watchlist (with games fetched to avoid N+1) ---
    @Query("""
           select s from GameSubscription s
           join fetch s.game
           where s.user.userId = :userId
           """)
    List<GameSubscription> findByUserIdWithGame(Long userId);

    // --- Bot: a user's watchlist (with games fetched to avoid N+1) ---

    // --- Bot: is this user already subscribed to this game? ---
    boolean existsByUser_UserIdAndGame_SteamAppId(Long userId, Long appId);

    // --- Bot: unsubscribe ---
    void deleteByUser_UserIdAndGame_SteamAppId(Long userId, Long appId);

    // --- Notification fan-out: who subscribes to this game? ---
    @Query("""
           select s from GameSubscription s
           join fetch s.user
           where s.game.steamAppId = :appId
           """)
    List<GameSubscription> findByAppIdWithUser(Long appId);

    @Query("""
           select s from GameSubscription s
           join fetch s.user
           where s.game.steamAppId = :appId
           and s.saleNotificationSent = false
           """)
    List<GameSubscription> findByAppIdWithUserUnnotified(Long appId);

    // --- Poller: check subscriber status for receiving price drop notification ---
    Integer countByGame_SteamAppIdAndSaleNotificationSent(Long appId, Boolean saleNotificationSent);

    @Modifying
    @Query("""
            update GameSubscription s set s.saleNotificationSent = false where s.game.steamAppId = :appId
           """)
    void resetNotificationSent(Long appId);
}
