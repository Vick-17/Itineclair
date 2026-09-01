package fr.itineclair.sharing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.itineclair.analysis.TrackAnalysisService;
import fr.itineclair.outdoor.OutdoorContextService;
import fr.itineclair.track.TrackImportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackShareServiceTest {

    private static final UUID OWNER_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");
    private static final Instant NOW =
            Instant.parse("2026-09-01T10:00:00Z");
    private static final String TOKEN = "A".repeat(43);
    private static final String TOKEN_HASH = "b".repeat(64);

    @Mock
    private TrackShareRepository shareRepository;

    @Mock
    private ShareTokenCodec tokenCodec;

    @Mock
    private TrackImportService trackImportService;

    @Mock
    private OutdoorContextService outdoorContextService;

    @Mock
    private TrackAnalysisService trackAnalysisService;

    private TrackShareService service;

    @BeforeEach
    void setUp() {
        service = new TrackShareService(
                shareRepository,
                tokenCodec,
                trackImportService,
                outdoorContextService,
                trackAnalysisService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsASevenDayShareWithoutPersistingTheRawToken() {
        given(tokenCodec.generate())
                .willReturn(new ShareTokenCodec.TokenMaterial(
                        TOKEN,
                        TOKEN_HASH));
        given(shareRepository.findByTrackIdAndOwnerId(
                TRACK_ID,
                OWNER_ID))
                .willReturn(Optional.empty());

        CreatedTrackShare created = service.createOrRotate(
                OWNER_ID,
                TRACK_ID,
                7);

        ArgumentCaptor<TrackShare> captor =
                ArgumentCaptor.forClass(TrackShare.class);
        verify(shareRepository).saveAndFlush(captor.capture());

        TrackShare persisted = captor.getValue();
        assertThat(created.token()).isEqualTo(TOKEN);
        assertThat(created.expiresAt())
                .isEqualTo(Instant.parse("2026-09-08T10:00:00Z"));
        assertThat(persisted.tokenHash())
                .isEqualTo(TOKEN_HASH)
                .doesNotContain(TOKEN);
    }

    @Test
    void expiredAndMissingTokensHaveTheSamePublicFailure() {
        TrackShare expired = TrackShare.create(
                TRACK_ID,
                OWNER_ID,
                TOKEN_HASH,
                NOW,
                NOW.minusSeconds(604_800));
        given(tokenCodec.hashPresented(TOKEN))
                .willReturn(Optional.of(TOKEN_HASH));
        given(shareRepository.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getPublicReport(TOKEN))
                .isInstanceOf(SharedReportNotFoundException.class);

        verify(trackAnalysisService, never())
                .analyze(OWNER_ID, TRACK_ID);
    }

    @Test
    void revocationFirstChecksTrackOwnership() {
        service.revoke(OWNER_ID, TRACK_ID);

        verify(trackImportService).getTrack(OWNER_ID, TRACK_ID);
        verify(shareRepository)
                .deleteByTrackIdAndOwnerId(TRACK_ID, OWNER_ID);
    }
}
