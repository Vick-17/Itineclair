package fr.itineclair.feedback;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TrackFeedbackServiceTest {
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID TRACK_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-31T12:00:00Z");
    @Mock TrackFeedbackRepository repository;
    @Mock TrackImportService tracks;
    private TrackFeedbackService service;

    @BeforeEach void setUp() {
        service = new TrackFeedbackService(repository, tracks,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void createsStructuredFeedback() {
        given(repository.findById(TRACK_ID)).willReturn(Optional.empty());
        given(repository.saveAndFlush(any())).willAnswer(call -> call.getArgument(0));
        TrackFeedbackView result = service.save(ACCOUNT_ID, TRACK_ID, command());
        assertThat(result.outcome()).isEqualTo(FeedbackOutcome.COMPLETED_WITH_CHANGES);
        assertThat(result.observedIssues()).containsExactlyInAnyOrder(
                FeedbackIssue.WEATHER, FeedbackIssue.FATIGUE);
        assertThat(result.createdAt()).isEqualTo(NOW);
        verify(tracks).getTrack(ACCOUNT_ID, TRACK_ID);
    }

    @Test void rejectsObservationsForNotStartedOutingBeforePersistence() {
        var invalid = new TrackFeedbackCommand(FeedbackOutcome.NOT_STARTED,
                30, null, ConditionsComparison.NOT_COMPARED, Set.of());
        assertThatThrownBy(() -> service.save(ACCOUNT_ID, TRACK_ID, invalid))
                .isInstanceOf(InvalidTrackFeedbackException.class)
                .hasMessageContaining("non démarrée");
        verify(repository, never()).findById(TRACK_ID);
    }

    @Test void hidesUnownedTrackBeforeReadingFeedback() {
        given(tracks.getTrack(ACCOUNT_ID, TRACK_ID)).willThrow(new TrackNotFoundException());
        assertThatThrownBy(() -> service.get(ACCOUNT_ID, TRACK_ID))
                .isInstanceOf(TrackNotFoundException.class);
        verifyNoInteractions(repository);
    }

    @Test void deletesOnlyAfterOwnershipCheck() {
        service.delete(ACCOUNT_ID, TRACK_ID);
        verify(tracks).getTrack(ACCOUNT_ID, TRACK_ID);
        verify(repository).deleteById(TRACK_ID);
    }

    private TrackFeedbackCommand command() {
        return new TrackFeedbackCommand(FeedbackOutcome.COMPLETED_WITH_CHANGES,
                405, 4, ConditionsComparison.WORSE_THAN_EXPECTED,
                Set.of(FeedbackIssue.WEATHER, FeedbackIssue.FATIGUE));
    }
}
