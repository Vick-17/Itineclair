package fr.itineclair.track;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TrackListServiceTest {

    private static final UUID OWNER_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");

    private static final Instant NOW =
            Instant.parse("2026-09-15T10:00:00Z");

    @Mock
    private TrackImportService trackImportService;

    @Mock
    private TrackListProgressStore progressStore;

    private TrackListService service;

    @BeforeEach
    void setUp() {
        service = new TrackListService(
                trackImportService,
                progressStore,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void returnsOneExplicitStatusPerTrackWithoutChangingListOrder() {
        TrackSummary pending = summary(1, false);
        TrackSummary toPlan = summary(2, true);
        TrackSummary toReview = summary(3, true);
        TrackSummary toDebrief = summary(4, true);
        TrackSummary recorded = summary(5, true);

        given(trackImportService.listTracks(OWNER_ID))
                .willReturn(List.of(
                        pending,
                        toPlan,
                        toReview,
                        toDebrief,
                        recorded));

        given(progressStore.findAllByOwnerId(OWNER_ID))
                .willReturn(Map.of(
                        toReview.id(),
                        new TrackListProgress(
                                Instant.parse("2026-09-16T08:00:00Z"),
                                360,
                                false),
                        toDebrief.id(),
                        new TrackListProgress(
                                Instant.parse("2026-09-14T08:00:00Z"),
                                360,
                                false),
                        recorded.id(),
                        new TrackListProgress(null, null, true)));

        assertThat(service.listTracks(OWNER_ID))
                .extracting(TrackListEntry::preparationStatus)
                .containsExactly(
                        TrackListStatus.ANALYSIS_PENDING,
                        TrackListStatus.DEPARTURE_TO_PLAN,
                        TrackListStatus.PREPARATION_TO_REVIEW,
                        TrackListStatus.FEEDBACK_TO_RECORD,
                        TrackListStatus.FEEDBACK_RECORDED);
    }

    @Test
    void avoidsTheProgressQueryWhenTheAccountHasNoTrack() {
        given(trackImportService.listTracks(OWNER_ID))
                .willReturn(List.of());

        assertThat(service.listTracks(OWNER_ID)).isEmpty();

        verifyNoInteractions(progressStore);
    }

    private TrackSummary summary(int suffix, boolean factsAvailable) {
        return new TrackSummary(
                UUID.fromString(
                        "00000000-0000-0000-0000-00000000000" + suffix),
                "Sortie " + suffix,
                "sortie-" + suffix + ".gpx",
                1,
                100,
                100,
                factsAvailable ? TrackFacts.CURRENT_VERSION : null,
                factsAvailable ? 12_000.0 : null,
                factsAvailable ? 700.0 : null,
                factsAvailable ? 680.0 : null,
                factsAvailable ? 900.0 : null,
                factsAvailable ? 1_600.0 : null,
                factsAvailable ? 12.0 : null,
                factsAvailable ? 14.0 : null,
                Instant.parse("2026-09-10T10:00:00Z"));
    }
}
