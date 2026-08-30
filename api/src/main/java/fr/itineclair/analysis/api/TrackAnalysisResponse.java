package fr.itineclair.analysis.api;

import java.time.Instant;
import java.util.List;

import fr.itineclair.analysis.RuleReviewStatus;
import fr.itineclair.analysis.TrackAnalysis;

public record TrackAnalysisResponse(
        int ruleSetVersion,
        RuleReviewStatus reviewStatus,
        Instant generatedAt,
        AnalysisSourceSnapshotResponse sourceSnapshot,
        List<AnalysisFindingResponse> findings,
        List<AnalysisChecklistItemResponse> checklist,
        List<String> limitations) {

    static TrackAnalysisResponse from(
            TrackAnalysis analysis) {
        return new TrackAnalysisResponse(
                analysis.ruleSetVersion(),
                analysis.reviewStatus(),
                analysis.generatedAt(),
                AnalysisSourceSnapshotResponse.from(
                        analysis.sourceSnapshot()),
                analysis.findings()
                        .stream()
                        .map(AnalysisFindingResponse::from)
                        .toList(),
                analysis.checklist()
                        .stream()
                        .map(AnalysisChecklistItemResponse::from)
                        .toList(),
                analysis.limitations());
    }
}
