package fr.itineclair.analysis;

import java.util.Objects;

public record AnalysisChecklistItem(
        String code,
        ChecklistStatus status,
        String title,
        String detail) {

    public AnalysisChecklistItem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(detail, "detail");
    }
}
