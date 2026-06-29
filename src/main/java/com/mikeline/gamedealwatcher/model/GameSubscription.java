package com.mikeline.gamedealwatcher.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "game_subscription",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private WatchlistUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private TrackedGame game;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt;

    @Setter
    @Column(name = "sale_notification_sent")
    private Boolean saleNotificationSent;

    public GameSubscription(WatchlistUser user, TrackedGame game, Boolean saleNotificationSent) {
        this.user = user;
        this.game = game;
        this.subscribedAt = Instant.now();
        this.saleNotificationSent = saleNotificationSent;
    }
}
