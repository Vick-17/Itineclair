package fr.itineclair.sharing.api;

import java.time.Instant;

import fr.itineclair.sharing.CreatedTrackShare;

public record CreatedTrackShareResponse(
        String token,
        Instant expiresAt,
        Instant createdAt) {

    static CreatedTrackShareResponse from(CreatedTrackShare share) {
        return new CreatedTrackShareResponse(
                share.token(),
                share.expiresAt(),
                share.createdAt());
    }
}
