package com.mikeline.gamedealwatcher.exception;

import com.mikeline.gamedealwatcher.model.IdentityProvider;

public class WatchlistUserNotFoundException extends RuntimeException {
    public WatchlistUserNotFoundException(IdentityProvider identityProvider, String extId) {
        super(identityProvider + " user ID " + extId + " not found");
    }
}
