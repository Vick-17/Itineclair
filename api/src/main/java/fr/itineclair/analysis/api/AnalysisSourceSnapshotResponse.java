package fr.itineclair.analysis.api;

import java.time.Instant;

import fr.itineclair.analysis.AnalysisSourceSnapshot;

public record AnalysisSourceSnapshotResponse(
        Integer factsVersion,
        Instant outdoorContextUpdatedAt,
        Instant weatherCheckedAt) {

    static AnalysisSourceSnapshotResponse from(
            AnalysisSourceSnapshot snapshot) {
        return new AnalysisSourceSnapshotResponse(
                snapshot.factsVersion(),
                snapshot.outdoorContextUpdatedAt(),
                snapshot.weatherCheckedAt());
    }
}
