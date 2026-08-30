package fr.itineclair.analysis;

public enum AnalysisSeverity {
    NOTICE(1),
    CAUTION(2),
    STRONG_CAUTION(3);

    private final int priority;

    AnalysisSeverity(int priority) {
        this.priority = priority;
    }

    int priority() {
        return priority;
    }
}
