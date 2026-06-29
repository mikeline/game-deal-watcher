package com.mikeline.gamedealwatcher.service;

import com.mikeline.gamedealwatcher.model.PriceHistory;
import com.mikeline.gamedealwatcher.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional(readOnly = true)
    public List<PriceHistory> findByAppId(Long appId) {
        return priceHistoryRepository.findAllBySteamAppIdOrderByRecordedAtDesc(appId);
    }

}
