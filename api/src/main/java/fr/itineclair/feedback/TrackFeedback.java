package fr.itineclair.feedback;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "track_feedbacks")
class TrackFeedback {

    @Id
    @Column(name = "track_id")
    private UUID trackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private FeedbackOutcome outcome;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "perceived_effort")
    private Integer perceivedEffort;

    @Enumerated(EnumType.STRING)
    @Column(name = "conditions_comparison", nullable = false, length = 32)
    private ConditionsComparison conditionsComparison;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "track_feedback_issues",
            joinColumns = @JoinColumn(name = "track_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "issue", nullable = false, length = 24)
    private Set<FeedbackIssue> observedIssues = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TrackFeedback() {
        // Constructeur requis par JPA.
    }

    private TrackFeedback(UUID trackId, TrackFeedbackCommand command, Instant now) {
        this.trackId = trackId;
        this.createdAt = now;
        update(command, now);
    }

    static TrackFeedback create(
            UUID trackId,
            TrackFeedbackCommand command,
            Instant now) {
        return new TrackFeedback(trackId, command, now);
    }

    void update(TrackFeedbackCommand command, Instant now) {
        outcome = command.outcome();
        actualDurationMinutes = command.actualDurationMinutes();
        perceivedEffort = command.perceivedEffort();
        conditionsComparison = command.conditionsComparison();
        observedIssues.clear();
        observedIssues.addAll(command.observedIssues());
        updatedAt = now;
    }

    UUID trackId() { return trackId; }
    FeedbackOutcome outcome() { return outcome; }
    Integer actualDurationMinutes() { return actualDurationMinutes; }
    Integer perceivedEffort() { return perceivedEffort; }
    ConditionsComparison conditionsComparison() { return conditionsComparison; }
    Set<FeedbackIssue> observedIssues() { return Set.copyOf(observedIssues); }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
