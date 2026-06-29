package com.mikeline.gamedealwatcher.service;

import com.mikeline.gamedealwatcher.exception.WatchlistUserNotFoundException;
import com.mikeline.gamedealwatcher.model.IdentityProvider;
import com.mikeline.gamedealwatcher.model.WatchlistUser;
import com.mikeline.gamedealwatcher.repository.WatchlistUserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistUserService {

    private final WatchlistUserRepository watchlistUserRepository;

    @Transactional
    public WatchlistUser registerIfNew(IdentityProvider identityProvider, String extId, String username) {
        return watchlistUserRepository.findByIdentityProviderAndExtId(identityProvider, extId)
                .orElseGet(() -> watchlistUserRepository.save(
                                                        new WatchlistUser(identityProvider, extId, username)));
    }

    @Transactional(readOnly = true)
    public WatchlistUser require(IdentityProvider identityProvider, String extId) {
        return watchlistUserRepository.findByIdentityProviderAndExtId(identityProvider, extId)
                .orElseThrow(() -> new WatchlistUserNotFoundException(identityProvider, extId));
    }
}
