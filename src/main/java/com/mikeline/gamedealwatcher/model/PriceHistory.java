package com.mikeline.gamedealwatcher.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "price_history",
        indexes = @Index(name = "idx_price_history_app_id", columnList = "steam_app_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_app_id", nullable = false)
    private Long steamAppId;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "final_price", nullable = false)
    private Integer finalPrice;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public PriceHistory(Long appId, String currency, int finalPrice, int discountPercent) {
        this.steamAppId = appId;
        this.currency = currency;
        this.finalPrice = finalPrice;
        this.discountPercent = discountPercent;
        this.recordedAt = Instant.now();
    }
}
