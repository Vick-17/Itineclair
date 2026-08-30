package fr.itineclair.analysis;

import java.util.Objects;

public record AnalysisEvidence(
        String metric,
        String label,
        double observedValue,
        String unit,
        EvidenceComparison comparison,
        double thresholdValue) {

    public AnalysisEvidence {
        Objects.requireNonNull(metric, "metric");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(comparison, "comparison");

        if (!Double.isFinite(observedValue)
                || !Double.isFinite(thresholdValue)) {
            throw new IllegalArgumentException(
                    "Analysis evidence values must be finite.");
        }
    }
}
