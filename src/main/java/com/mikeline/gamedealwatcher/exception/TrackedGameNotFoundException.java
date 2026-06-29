package com.mikeline.gamedealwatcher.exception;

public class TrackedGameNotFoundException extends RuntimeException {
    public TrackedGameNotFoundException(Long appId) {
        super("App " + appId + " is not on the watchlist");
    }
}
