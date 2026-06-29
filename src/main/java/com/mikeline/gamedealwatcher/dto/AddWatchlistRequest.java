package com.mikeline.gamedealwatcher.dto;

import jakarta.validation.constraints.NotNull;

public record AddWatchlistRequest(
        @NotNull(message = "appId is required") Long appId,
        String appName
) {
}
