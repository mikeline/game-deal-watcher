package com.mikeline.gamedealwatcher.api;

import com.mikeline.gamedealwatcher.dto.AddWatchlistRequest;
import com.mikeline.gamedealwatcher.dto.WatchlistResponse;
import com.mikeline.gamedealwatcher.service.TrackedGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final TrackedGameService service;

    @GetMapping
    public List<WatchlistResponse> list() {
        throw new UnsupportedOperationException();
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> add(@Valid @RequestBody AddWatchlistRequest request) {
        //WatchlistResponse body = WatchlistResponse.from(service.add(request));
        //return ResponseEntity.status(HttpStatus.CREATED).body(body);
        throw new UnsupportedOperationException();
    }

    @DeleteMapping("/{appId}")
    public ResponseEntity<Void> remove(@PathVariable Long appId) {
        throw new UnsupportedOperationException();
    }
}
