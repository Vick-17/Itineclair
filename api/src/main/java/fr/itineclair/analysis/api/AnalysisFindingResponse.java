package fr.itineclair.analysis.api;

import java.util.List;

import fr.itineclair.analysis.AnalysisCategory;
import fr.itineclair.analysis.AnalysisFinding;
import fr.itineclair.analysis.AnalysisSeverity;

public record AnalysisFindingResponse(
        String code,
        AnalysisCategory category,
        AnalysisSeverity severity,
        String title,
        String explanation,
        String action,
        List<AnalysisEvidenceResponse> evidence) {

    static AnalysisFindingResponse from(
            AnalysisFinding finding) {
        return new AnalysisFindingResponse(
                finding.code(),
                finding.category(),
                finding.severity(),
                finding.title(),
                finding.explanation(),
                finding.action(),
                finding.evidence()
                        .stream()
                        .map(AnalysisEvidenceResponse::from)
                        .toList());
    }
}
