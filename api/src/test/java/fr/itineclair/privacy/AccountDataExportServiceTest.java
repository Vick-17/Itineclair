package fr.itineclair.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.security.CurrentPasswordVerifier;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AccountDataExportServiceTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");
    private static final UUID TRACK_ID =
            UUID.fromString("ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf");
    private static final Instant NOW =
            Instant.parse("2026-09-01T12:00:00Z");
    private static final String PASSWORD =
            "une phrase de passe de test suffisamment longue";

    @Mock private AccountDataStore accountDataStore;
    @Mock private CurrentPasswordVerifier currentPasswordVerifier;

    private AccountDataExportService service;
    private AccountPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new AccountDataExportService(
                accountDataStore,
                currentPasswordVerifier,
                JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        principal = mock(AccountPrincipal.class);
        lenient().when(principal.id()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void preparesAndStreamsASecretFreePortableArchive()
            throws IOException {
        AccountExportSnapshot snapshot = snapshot();
        given(currentPasswordVerifier.matches(principal, PASSWORD))
                .willReturn(true);
        given(accountDataStore.loadSnapshot(ACCOUNT_ID))
                .willReturn(Optional.of(snapshot));
        given(accountDataStore.loadTrackPoints(ACCOUNT_ID, TRACK_ID))
                .willReturn(List.of(
                        new AccountExportSnapshot.TrackPoint(
                                0, 0, 45.1, 6.1, 1200.0, NOW),
                        new AccountExportSnapshot.TrackPoint(
                                0, 1, 45.2, 6.2, 1260.0, null)));

        PreparedAccountExport prepared = service.prepareExport(
                principal,
                PASSWORD);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.writeExport(prepared, output);

        assertThat(prepared.filename())
                .isEqualTo("itineclair-export-20260901T120000Z.zip");

        List<ArchiveEntry> entries = unzip(output.toByteArray());
        assertThat(entries).extracting(ArchiveEntry::name)
                .containsExactly(
                        "manifest.json",
                        "tracks/" + TRACK_ID + ".gpx");

        String manifest = entries.getFirst().content();
        assertThat(manifest)
                .contains("itineclair-account-export")
                .contains("\"schemaVersion\":2")
                .contains("victor@example.test")
                .contains("EXPERIENCED")
                .contains("COMPLETED_AS_PLANNED")
                .contains("WEATHER")
                .doesNotContain(PASSWORD)
                .doesNotContain("passwordHash")
                .doesNotContain("tokenHash");

        assertThat(entries.get(1).content())
                .contains("<gpx")
                .contains("<trkseg>")
                .contains("lat=\"45.1\"")
                .contains("<ele>1200.0</ele>")
                .doesNotContain("token");
    }

    @Test
    void wrongPasswordStopsBeforeAnyDataRead() {
        given(currentPasswordVerifier.matches(principal, PASSWORD))
                .willReturn(false);

        assertThatThrownBy(() -> service.prepareExport(
                principal,
                PASSWORD))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .hasMessageNotContaining(PASSWORD);

        verify(accountDataStore, never()).loadSnapshot(ACCOUNT_ID);
    }

    private AccountExportSnapshot snapshot() {
        var feedback = new AccountExportSnapshot.Feedback(
                "COMPLETED_AS_PLANNED",
                180,
                3,
                "AS_EXPECTED",
                List.of("WEATHER"),
                NOW.minusSeconds(3600),
                NOW);
        var track = new AccountExportSnapshot.Track(
                TRACK_ID,
                "Boucle test",
                "boucle.gpx",
                1,
                2,
                2,
                NOW.minusSeconds(7200),
                new AccountExportSnapshot.TrackFacts(
                        1, 2450.0, 120.0, 120.0,
                        1200.0, 1320.0, 18.0, 17.0),
                null,
                feedback,
                new AccountExportSnapshot.PrivateShare(
                        NOW.minusSeconds(600),
                        NOW.plusSeconds(604800)),
                "tracks/" + TRACK_ID + ".gpx");

        return new AccountExportSnapshot(
                new AccountExportSnapshot.Account(
                        ACCOUNT_ID,
                        "victor@example.test",
                        NOW.minusSeconds(86_400)),
                new AccountExportSnapshot.HikerProfile(
                        "EXPERIENCED",
                        360,
                        14_000,
                        900,
                        NOW.minusSeconds(43_200),
                        NOW.minusSeconds(3_600)),
                List.of(track));
    }

    private List<ArchiveEntry> unzip(byte[] archive) throws IOException {
        List<ArchiveEntry> entries = new ArrayList<>();

        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(archive),
                StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(new ArchiveEntry(
                        entry.getName(),
                        new String(zip.readAllBytes(), StandardCharsets.UTF_8)));
            }
        }

        return entries;
    }

    private record ArchiveEntry(String name, String content) {
    }
}
