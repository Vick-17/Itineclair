package fr.itineclair.feedback;

import java.util.Set;

public record TrackFeedbackCommand(
        FeedbackOutcome outcome,
        Integer actualDurationMinutes,
        Integer perceivedEffort,
        ConditionsComparison conditionsComparison,
        Set<FeedbackIssue> observedIssues) {
}
