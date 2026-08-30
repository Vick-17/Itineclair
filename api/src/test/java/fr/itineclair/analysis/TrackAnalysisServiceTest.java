package fr.itineclair.analysis;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.itineclair.outdoor.OutdoorContextService;
import fr.itineclair.track.TrackImportService;
import fr.itineclair.track.TrackSummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrackAnalysisServiceTest {

    private static final UUID OWNER_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");
    private static final Instant NOW =
            Instant.parse("2026-08-30T12:00:00Z");

    @Test
    void usesOwnerScopedServicesForEveryAnalysisInput() {
        TrackImportService trackImportService =
                mock(TrackImportService.class);
        OutdoorContextService outdoorContextService =
                mock(OutdoorContextService.class);
        TrackSummary track = track();
        given(trackImportService.getTrack(OWNER_ID, TRACK_ID))
                .willReturn(track);
        given(outdoorContextService.getContext(OWNER_ID, TRACK_ID))
                .willReturn(Optional.empty());

        TrackAnalysisService service = new TrackAnalysisService(
                trackImportService,
                outdoorContextService,
                new TrackRuleAnalysisEngine(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        TrackAnalysis analysis = service.analyze(OWNER_ID, TRACK_ID);

        assertThat(analysis.generatedAt()).isEqualTo(NOW);
        verify(trackImportService).getTrack(OWNER_ID, TRACK_ID);
        verify(outdoorContextService).getContext(OWNER_ID, TRACK_ID);
    }

    private TrackSummary track() {
        return new TrackSummary(
                TRACK_ID,
                "Boucle test",
                "boucle.gpx",
                1,
                100,
                100,
                1,
                12_000.0,
                500.0,
                500.0,
                1_000.0,
                1_800.0,
                20.0,
                20.0,
                NOW.minusSeconds(86_400));
    }
}
