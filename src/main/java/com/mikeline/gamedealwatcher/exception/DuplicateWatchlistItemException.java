package com.mikeline.gamedealwatcher.exception;

public class DuplicateWatchlistItemException extends RuntimeException {
    public DuplicateWatchlistItemException(Long appId) {
        super("App " + appId + " is already on the watchlist");
    }
}
