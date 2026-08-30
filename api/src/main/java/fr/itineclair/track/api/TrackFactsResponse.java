package fr.itineclair.track.api;

import fr.itineclair.track.TrackFacts;
import fr.itineclair.track.TrackSummary;

public record TrackFactsResponse(
        double distanceMeters,
        Double elevationGainMeters,
        Double elevationLossMeters,
        Double minimumElevationMeters,
        Double maximumElevationMeters,
        Double maximumUphillGradePercent,
        Double maximumDownhillGradePercent,
        int gradeMinimumRunMeters) {

    static TrackFactsResponse from(TrackSummary track) {
        if (!track.factsAvailable()) {
            return null;
        }

        return new TrackFactsResponse(
                track.totalDistanceMeters(),
                track.elevationGainMeters(),
                track.elevationLossMeters(),
                track.minimumElevationMeters(),
                track.maximumElevationMeters(),
                track.maximumUphillGradePercent(),
                track.maximumDownhillGradePercent(),
                TrackFacts.MINIMUM_GRADE_RUN_METERS);
    }
}
