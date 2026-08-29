package fr.itineclair.track.api;

import java.time.Instant;
import java.util.UUID;

import fr.itineclair.track.TrackSummary;

public record TrackResponse(
        UUID id,
        String name,
        String sourceFilename,
        int segmentCount,
        int pointCount,
        int elevationPointCount,
        boolean elevationComplete,
        Instant createdAt) {

    static TrackResponse from(TrackSummary track) {
        return new TrackResponse(
                track.id(),
                track.name(),
                track.sourceFilename(),
                track.segmentCount(),
                track.pointCount(),
                track.elevationPointCount(),
                track.elevationPointCount() == track.pointCount(),
                track.createdAt());
    }
}
