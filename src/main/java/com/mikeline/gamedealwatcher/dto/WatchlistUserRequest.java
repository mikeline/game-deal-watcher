package com.mikeline.gamedealwatcher.dto;

import com.mikeline.gamedealwatcher.model.IdentityProvider;
import jakarta.validation.constraints.NotNull;

public record WatchlistUserRequest(
        @NotNull(message = "identityProvider is required") IdentityProvider identityProvider,
        @NotNull(message = "extId is required") String extId) {
}
