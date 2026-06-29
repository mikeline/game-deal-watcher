package com.mikeline.gamedealwatcher.repository;

import com.mikeline.gamedealwatcher.model.IdentityProvider;
import com.mikeline.gamedealwatcher.model.WatchlistUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchlistUserRepository extends JpaRepository<WatchlistUser, Long> {

    Optional<WatchlistUser> findByIdentityProviderAndExtId(IdentityProvider identityProvider, String extId);

    boolean existsByIdentityProviderAndExtId(IdentityProvider identityProvider, String extId);

}
