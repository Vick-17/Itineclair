package fr.itineclair.track.api;

import java.time.Instant;
import java.util.UUID;

import fr.itineclair.track.TrackListEntry;
import fr.itineclair.track.TrackListStatus;
import fr.itineclair.track.TrackSummary;

public record TrackListItemResponse(
        UUID id,
        String name,
        String sourceFilename,
        int segmentCount,
        int pointCount,
        int elevationPointCount,
        boolean elevationComplete,
        TrackFactsResponse facts,
        Instant createdAt,
        TrackListStatus preparationStatus) {

    static TrackListItemResponse from(TrackListEntry entry) {
        TrackSummary track = entry.track();

        return new TrackListItemResponse(
                track.id(),
                track.name(),
                track.sourceFilename(),
                track.segmentCount(),
                track.pointCount(),
                track.elevationPointCount(),
                track.elevationPointCount()
                        == track.pointCount(),
                TrackFactsResponse.from(track),
                track.createdAt(),
                entry.preparationStatus());
    }
}
