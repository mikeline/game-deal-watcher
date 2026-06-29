package com.mikeline.gamedealwatcher.api;

import com.mikeline.gamedealwatcher.dto.PriceHistoryResponse;
import com.mikeline.gamedealwatcher.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/watchlist/history")
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping("/{appId}")
    public List<PriceHistoryResponse> findByAppId(@PathVariable Long appId) {
        return priceHistoryService.findByAppId(appId).stream()
                .map(PriceHistoryResponse::from)
                .toList();
    }
}
