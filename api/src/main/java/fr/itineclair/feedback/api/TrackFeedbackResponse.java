package fr.itineclair.feedback.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import fr.itineclair.feedback.ConditionsComparison;
import fr.itineclair.feedback.FeedbackIssue;
import fr.itineclair.feedback.FeedbackOutcome;
import fr.itineclair.feedback.TrackFeedbackView;

public record TrackFeedbackResponse(
        boolean recorded,
        UUID trackId,
        FeedbackOutcome outcome,
        Integer actualDurationMinutes,
        Integer perceivedEffort,
        ConditionsComparison conditionsComparison,
        List<FeedbackIssue> observedIssues,
        Instant createdAt,
        Instant updatedAt) {

    static TrackFeedbackResponse notRecorded(UUID trackId) {
        return new TrackFeedbackResponse(
                false, trackId, null, null, null, null, List.of(), null, null);
    }

    static TrackFeedbackResponse from(TrackFeedbackView feedback) {
        return new TrackFeedbackResponse(
                true,
                feedback.trackId(),
                feedback.outcome(),
                feedback.actualDurationMinutes(),
                feedback.perceivedEffort(),
                feedback.conditionsComparison(),
                feedback.observedIssues().stream().sorted().toList(),
                feedback.createdAt(),
                feedback.updatedAt());
    }
}
