package fr.itineclair.analysis.api;

import fr.itineclair.analysis.AnalysisChecklistItem;
import fr.itineclair.analysis.ChecklistStatus;

public record AnalysisChecklistItemResponse(
        String code,
        ChecklistStatus status,
        String title,
        String detail) {

    static AnalysisChecklistItemResponse from(
            AnalysisChecklistItem item) {
        return new AnalysisChecklistItemResponse(
                item.code(),
                item.status(),
                item.title(),
                item.detail());
    }
}
