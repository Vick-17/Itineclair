package fr.itineclair.feedback;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TrackFeedbackView(
        UUID trackId,
        FeedbackOutcome outcome,
        Integer actualDurationMinutes,
        Integer perceivedEffort,
        ConditionsComparison conditionsComparison,
        Set<FeedbackIssue> observedIssues,
        Instant createdAt,
        Instant updatedAt) {

    static TrackFeedbackView from(TrackFeedback feedback) {
        return new TrackFeedbackView(
                feedback.trackId(),
                feedback.outcome(),
                feedback.actualDurationMinutes(),
                feedback.perceivedEffort(),
                feedback.conditionsComparison(),
                Set.copyOf(feedback.observedIssues()),
                feedback.createdAt(),
                feedback.updatedAt());
    }
}
