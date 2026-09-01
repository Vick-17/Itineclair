package fr.itineclair.sharing;

import java.time.Instant;

public record CreatedTrackShare(
        String token,
        Instant expiresAt,
        Instant createdAt) {
}
