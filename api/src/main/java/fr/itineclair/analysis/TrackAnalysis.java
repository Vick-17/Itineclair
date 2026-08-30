package fr.itineclair.analysis;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TrackAnalysis(
        int ruleSetVersion,
        RuleReviewStatus reviewStatus,
        Instant generatedAt,
        AnalysisSourceSnapshot sourceSnapshot,
        List<AnalysisFinding> findings,
        List<AnalysisChecklistItem> checklist,
        List<String> limitations) {

    public static final int CURRENT_RULE_SET_VERSION = 1;

    public TrackAnalysis {
        if (ruleSetVersion != CURRENT_RULE_SET_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported analysis rule set version.");
        }

        Objects.requireNonNull(reviewStatus, "reviewStatus");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(sourceSnapshot, "sourceSnapshot");
        findings = List.copyOf(findings);
        checklist = List.copyOf(checklist);
        limitations = List.copyOf(limitations);
    }
}
