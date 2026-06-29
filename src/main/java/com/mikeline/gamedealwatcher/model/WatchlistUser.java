package com.mikeline.gamedealwatcher.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "watchlist_user",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_provider_ext_id",
               columnNames = {"identity_provider", "ext_id"}
       ))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WatchlistUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_provider", nullable = false, length = 32)
    private IdentityProvider identityProvider;

    @Column(name = "ext_id", nullable = false)
    private String extId;

    @Column(name = "username")
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WatchlistUser(IdentityProvider identityProvider, String extId, String username) {
        this.identityProvider = identityProvider;
        this.extId = extId;
        this.username = username;
        this.createdAt = Instant.now();
    }


}
