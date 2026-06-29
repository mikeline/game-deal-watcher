package com.mikeline.gamedealwatcher.repository;

import com.mikeline.gamedealwatcher.model.TrackedGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackedGameRepository extends JpaRepository<TrackedGame, Long> {

    Optional<TrackedGame> findBySteamAppId(Long appId);

    boolean existsBySteamAppId(Long appId);

    void deleteBySteamAppId(Long appId);
}
