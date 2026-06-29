package com.mikeline.gamedealwatcher.repository;

import com.mikeline.gamedealwatcher.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    Optional<PriceHistory> findTopBySteamAppIdOrderByRecordedAtDesc(Long appId);

    List<PriceHistory> findAllBySteamAppIdOrderByRecordedAtDesc(Long appId);
}
