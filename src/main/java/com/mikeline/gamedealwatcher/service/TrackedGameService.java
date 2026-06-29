package com.mikeline.gamedealwatcher.service;

import com.mikeline.gamedealwatcher.dto.AddWatchlistRequest;
import com.mikeline.gamedealwatcher.exception.DuplicateWatchlistItemException;
import com.mikeline.gamedealwatcher.exception.TrackedGameNotFoundException;
import com.mikeline.gamedealwatcher.model.TrackedGame;
import com.mikeline.gamedealwatcher.repository.TrackedGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackedGameService {

    private final TrackedGameRepository repository;

    @Transactional(readOnly = true)
    public TrackedGame findBySteamAppId(Long appId) {
        return repository.findBySteamAppId(appId).orElseThrow(() -> new TrackedGameNotFoundException(appId));
    }

    @Transactional(readOnly = true)
    public boolean existsBySteamAppId(Long appId) {
        return repository.existsBySteamAppId(appId);
    }

    @Transactional
    public TrackedGame add(AddWatchlistRequest request) {
        if (repository.existsBySteamAppId(request.appId())) {
            throw new DuplicateWatchlistItemException(request.appId());
        }
        TrackedGame item = new TrackedGame(request.appId(), request.appName());
        return repository.save(item);
    }

    @Transactional
    public void remove(Long appId) {
        if (!repository.existsBySteamAppId(appId)) {
            throw new TrackedGameNotFoundException(appId);
        }
        repository.deleteBySteamAppId(appId);
    }
}
