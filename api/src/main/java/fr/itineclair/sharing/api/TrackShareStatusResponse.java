package fr.itineclair.sharing.api;

import java.time.Instant;

import fr.itineclair.sharing.TrackShareStatus;

public record TrackShareStatusResponse(
        boolean active,
        Instant expiresAt,
        Instant createdAt) {

    static TrackShareStatusResponse from(TrackShareStatus status) {
        return new TrackShareStatusResponse(
                status.active(),
                status.expiresAt(),
                status.createdAt());
    }
}
