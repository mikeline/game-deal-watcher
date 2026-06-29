package com.mikeline.gamedealwatcher.dto;

import com.mikeline.gamedealwatcher.model.TrackedGame;

import java.time.Instant;

public record WatchlistResponse(
        Long id,
        Long appId,
        String appName,
        Instant addedAt
) {
    public static WatchlistResponse from(TrackedGame item) {
        return new WatchlistResponse(
                item.getId(),
                item.getSteamAppId(),
                item.getAppName(),
                item.getAddedAt()
        );
    }
}
