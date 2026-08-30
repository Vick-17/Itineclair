package fr.itineclair.analysis;

import java.util.List;

final class AnalysisRuleSupport {

    private AnalysisRuleSupport() {
    }

    static void addOptionalHighThresholdFinding(
            List<AnalysisFinding> findings,
            String code,
            AnalysisCategory category,
            String title,
            String explanation,
            String action,
            String metric,
            String label,
            Double observedValue,
            String unit,
            double cautionThreshold,
            double strongThreshold) {
        if (observedValue == null) {
            return;
        }

        addHighThresholdFinding(
                findings,
                code,
                category,
                title,
                explanation,
                action,
                metric,
                label,
                observedValue,
                unit,
                cautionThreshold,
                strongThreshold);
    }

    static void addHighThresholdFinding(
            List<AnalysisFinding> findings,
            String code,
            AnalysisCategory category,
            String title,
            String explanation,
            String action,
            String metric,
            String label,
            double observedValue,
            String unit,
            double cautionThreshold,
            double strongThreshold) {
        if (observedValue < cautionThreshold) {
            return;
        }

        AnalysisSeverity severity = observedValue >= strongThreshold
                ? AnalysisSeverity.STRONG_CAUTION
                : AnalysisSeverity.CAUTION;
        double threshold = severity == AnalysisSeverity.STRONG_CAUTION
                ? strongThreshold
                : cautionThreshold;
        findings.add(new AnalysisFinding(
                code,
                category,
                severity,
                title,
                explanation,
                action,
                List.of(evidence(
                        metric,
                        label,
                        observedValue,
                        unit,
                        EvidenceComparison.GREATER_OR_EQUAL,
                        threshold))));
    }

    static void addOptionalLowThresholdFinding(
            List<AnalysisFinding> findings,
            String code,
            AnalysisCategory category,
            String title,
            String explanation,
            String action,
            String metric,
            String label,
            Double observedValue,
            String unit,
            double cautionThreshold,
            double strongThreshold) {
        if (observedValue == null || observedValue > cautionThreshold) {
            return;
        }

        AnalysisSeverity severity = observedValue <= strongThreshold
                ? AnalysisSeverity.STRONG_CAUTION
                : AnalysisSeverity.CAUTION;
        double threshold = severity == AnalysisSeverity.STRONG_CAUTION
                ? strongThreshold
                : cautionThreshold;
        findings.add(new AnalysisFinding(
                code,
                category,
                severity,
                title,
                explanation,
                action,
                List.of(evidence(
                        metric,
                        label,
                        observedValue,
                        unit,
                        EvidenceComparison.LESS_OR_EQUAL,
                        threshold))));
    }

    static AnalysisEvidence evidence(
            String metric,
            String label,
            double observedValue,
            String unit,
            EvidenceComparison comparison,
            double thresholdValue) {
        return new AnalysisEvidence(
                metric,
                label,
                observedValue,
                unit,
                comparison,
                thresholdValue);
    }

    static Double maximum(Double first, Double second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return Math.max(first, second);
    }

    static boolean atLeast(Number value, double threshold) {
        return value != null && value.doubleValue() >= threshold;
    }
}
