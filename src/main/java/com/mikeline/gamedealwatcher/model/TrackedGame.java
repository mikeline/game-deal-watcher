package com.mikeline.gamedealwatcher.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "tracked_game")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackedGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_app_id", nullable = false, unique = true)
    private Long steamAppId;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    public TrackedGame(Long appId, String appName) {
        this.steamAppId = appId;
        this.appName = appName;
        this.addedAt = Instant.now();
    }
}
