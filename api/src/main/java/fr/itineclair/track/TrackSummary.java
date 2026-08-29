package fr.itineclair.track;

import java.time.Instant;
import java.util.UUID;

public record TrackSummary(
        UUID id,
        String name,
        String sourceFilename,
        int segmentCount,
        int pointCount,
        int elevationPointCount,
        Instant createdAt) {

    static TrackSummary from(Track track) {
        return new TrackSummary(
                track.id(),
                track.name(),
                track.sourceFilename(),
                track.segmentCount(),
                track.pointCount(),
                track.elevationPointCount(),
                track.createdAt());
    }
}
