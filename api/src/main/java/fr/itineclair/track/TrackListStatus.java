package fr.itineclair.track;

import java.time.Instant;

public enum TrackListStatus {
    ANALYSIS_PENDING,
    DEPARTURE_TO_PLAN,
    PREPARATION_TO_REVIEW,
    FEEDBACK_TO_RECORD,
    FEEDBACK_RECORDED;

    static TrackListStatus resolve(
            TrackSummary track,
            TrackListProgress progress,
            Instant now) {
        if (!track.factsAvailable()) {
            return ANALYSIS_PENDING;
        }

        if (progress.feedbackRecorded()) {
            return FEEDBACK_RECORDED;
        }

        Instant plannedEndAt = progress.plannedEndAt();

        if (plannedEndAt == null) {
            return DEPARTURE_TO_PLAN;
        }

        if (!plannedEndAt.isAfter(now)) {
            return FEEDBACK_TO_RECORD;
        }

        return PREPARATION_TO_REVIEW;
    }
}
