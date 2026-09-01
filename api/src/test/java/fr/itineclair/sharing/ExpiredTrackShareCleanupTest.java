package fr.itineclair.sharing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpiredTrackShareCleanupTest {

    @Test
    void deletesExpiredHashRowsUsingUtcClock() {
        Instant now = Instant.parse("2026-09-01T03:17:00Z");
        TrackShareRepository repository =
                mock(TrackShareRepository.class);
        ExpiredTrackShareCleanup cleanup =
                new ExpiredTrackShareCleanup(
                        repository,
                        Clock.fixed(now, ZoneOffset.UTC));

        cleanup.deleteExpiredShares();

        verify(repository).deleteByExpiresAtLessThanEqual(now);
    }
}
