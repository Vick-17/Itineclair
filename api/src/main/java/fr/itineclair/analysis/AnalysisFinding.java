package fr.itineclair.analysis;

import java.util.List;
import java.util.Objects;

public record AnalysisFinding(
        String code,
        AnalysisCategory category,
        AnalysisSeverity severity,
        String title,
        String explanation,
        String action,
        List<AnalysisEvidence> evidence) {

    public AnalysisFinding {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(explanation, "explanation");
        Objects.requireNonNull(action, "action");
        evidence = List.copyOf(evidence);
    }
}
