package fr.itineclair.feedback;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.itineclair.track.TrackImportService;

@Service
public class TrackFeedbackService {

    private final TrackFeedbackRepository feedbackRepository;
    private final TrackImportService trackImportService;
    private final Clock clock;

    public TrackFeedbackService(
            TrackFeedbackRepository feedbackRepository,
            TrackImportService trackImportService,
            Clock clock) {
        this.feedbackRepository = feedbackRepository;
        this.trackImportService = trackImportService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<TrackFeedbackView> get(UUID ownerId, UUID trackId) {
        requireOwnedTrack(ownerId, trackId);
        return feedbackRepository.findById(trackId).map(TrackFeedbackView::from);
    }

    @Transactional
    public TrackFeedbackView save(
            UUID ownerId,
            UUID trackId,
            TrackFeedbackCommand command) {
        Objects.requireNonNull(command, "command");
        validate(command);
        requireOwnedTrack(ownerId, trackId);

        Instant now = clock.instant();
        TrackFeedback feedback = feedbackRepository.findById(trackId)
                .orElseGet(() -> TrackFeedback.create(trackId, command, now));
        feedback.update(command, now);
        return TrackFeedbackView.from(feedbackRepository.saveAndFlush(feedback));
    }

    @Transactional
    public void delete(UUID ownerId, UUID trackId) {
        requireOwnedTrack(ownerId, trackId);
        feedbackRepository.deleteById(trackId);
    }

    private void requireOwnedTrack(UUID ownerId, UUID trackId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(trackId, "trackId");
        trackImportService.getTrack(ownerId, trackId);
    }

    private void validate(TrackFeedbackCommand command) {
        if (command.outcome() == null) {
            throw new InvalidTrackFeedbackException("Le résultat de la sortie est obligatoire.");
        }
        if (command.conditionsComparison() == null) {
            throw new InvalidTrackFeedbackException("La comparaison des conditions est obligatoire.");
        }
        Set<FeedbackIssue> issues = command.observedIssues();
        if (issues == null
                || issues.stream().anyMatch(Objects::isNull)
                || issues.size() > 5) {
            throw new InvalidTrackFeedbackException("La liste des difficultés observées est invalide.");
        }
        if (command.actualDurationMinutes() != null
                && (command.actualDurationMinutes() < 1
                || command.actualDurationMinutes() > 1440)) {
            throw new InvalidTrackFeedbackException("La durée réelle doit être comprise entre 1 minute et 24 heures.");
        }
        if (command.perceivedEffort() != null
                && (command.perceivedEffort() < 1 || command.perceivedEffort() > 5)) {
            throw new InvalidTrackFeedbackException("Le ressenti doit être compris entre 1 et 5.");
        }
        if (command.outcome() == FeedbackOutcome.NOT_STARTED
                && (command.actualDurationMinutes() != null
                || command.perceivedEffort() != null
                || command.conditionsComparison() != ConditionsComparison.NOT_COMPARED
                || !issues.isEmpty())) {
            throw new InvalidTrackFeedbackException(
                    "Une sortie non démarrée ne peut pas contenir d’observations terrain.");
        }
    }
}
