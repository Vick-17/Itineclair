package fr.itineclair.sharing;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "track_shares")
class TrackShare {

    @Id
    @Column(name = "track_id")
    private UUID trackId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TrackShare() {
        // Constructeur requis par JPA.
    }

    private TrackShare(
            UUID trackId,
            UUID ownerId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt) {
        this.trackId = trackId;
        this.ownerId = ownerId;
        activate(tokenHash, expiresAt, createdAt);
    }

    static TrackShare create(
            UUID trackId,
            UUID ownerId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt) {
        return new TrackShare(
                trackId,
                ownerId,
                tokenHash,
                expiresAt,
                createdAt);
    }

    void activate(
            String tokenHash,
            Instant expiresAt,
            Instant createdAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    boolean isActiveAt(Instant instant) {
        return expiresAt.isAfter(instant);
    }

    UUID trackId() {
        return trackId;
    }

    UUID ownerId() {
        return ownerId;
    }

    String tokenHash() {
        return tokenHash;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    Instant createdAt() {
        return createdAt;
    }
}
