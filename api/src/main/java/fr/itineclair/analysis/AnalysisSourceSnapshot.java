package fr.itineclair.analysis;

import java.time.Instant;

public record AnalysisSourceSnapshot(
        Integer factsVersion,
        Instant outdoorContextUpdatedAt,
        Instant weatherCheckedAt) {
}
