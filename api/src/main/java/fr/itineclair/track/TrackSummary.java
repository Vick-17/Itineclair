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
        Integer factsVersion,
        Double totalDistanceMeters,
        Double elevationGainMeters,
        Double elevationLossMeters,
        Double minimumElevationMeters,
        Double maximumElevationMeters,
        Double maximumUphillGradePercent,
        Double maximumDownhillGradePercent,
        Instant createdAt) {

    static TrackSummary from(Track track) {
        return new TrackSummary(
                track.id(),
                track.name(),
                track.sourceFilename(),
                track.segmentCount(),
                track.pointCount(),
                track.elevationPointCount(),
                track.factsVersion(),
                track.totalDistanceMeters(),
                track.elevationGainMeters(),
                track.elevationLossMeters(),
                track.minimumElevationMeters(),
                track.maximumElevationMeters(),
                track.maximumUphillGradePercent(),
                track.maximumDownhillGradePercent(),
                track.createdAt());
    }

    public boolean factsAvailable() {
        return Integer.valueOf(TrackFacts.CURRENT_VERSION)
                .equals(factsVersion)
                && totalDistanceMeters != null;
    }
}
