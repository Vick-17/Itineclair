package fr.itineclair.track;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

record TrackListProgress(
        Instant plannedStartAt,
        Integer plannedDurationMinutes,
        boolean feedbackRecorded) {

    static TrackListProgress empty() {
        return new TrackListProgress(null, null, false);
    }

    Instant plannedEndAt() {
        if (plannedStartAt == null || plannedDurationMinutes == null) {
            return null;
        }

        return plannedStartAt.plus(
                plannedDurationMinutes,
                ChronoUnit.MINUTES);
    }
}
