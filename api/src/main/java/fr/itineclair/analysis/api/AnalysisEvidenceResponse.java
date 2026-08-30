package fr.itineclair.analysis.api;

import fr.itineclair.analysis.AnalysisEvidence;
import fr.itineclair.analysis.EvidenceComparison;

public record AnalysisEvidenceResponse(
        String metric,
        String label,
        double observedValue,
        String unit,
        EvidenceComparison comparison,
        double thresholdValue) {

    static AnalysisEvidenceResponse from(
            AnalysisEvidence evidence) {
        return new AnalysisEvidenceResponse(
                evidence.metric(),
                evidence.label(),
                evidence.observedValue(),
                evidence.unit(),
                evidence.comparison(),
                evidence.thresholdValue());
    }
}
