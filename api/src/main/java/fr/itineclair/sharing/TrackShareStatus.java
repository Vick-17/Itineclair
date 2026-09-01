package fr.itineclair.sharing;

import java.time.Instant;

public record TrackShareStatus(
        boolean active,
        Instant expiresAt,
        Instant createdAt) {

    static TrackShareStatus inactive() {
        return new TrackShareStatus(false, null, null);
    }

    static TrackShareStatus active(TrackShare share) {
        return new TrackShareStatus(
                true,
                share.expiresAt(),
                share.createdAt());
    }
}
