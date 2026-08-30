package fr.itineclair.outdoor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import fr.itineclair.track.TrackStartPoint;

import static org.assertj.core.api.Assertions.assertThat;

class DaylightCalculatorTest {

    private static final TrackStartPoint PARIS =
            new TrackStartPoint(48.8566, 2.3522, 35.0);
    private static final ZoneId PARIS_TIME = ZoneId.of("Europe/Paris");

    private final DaylightCalculator calculator =
            new DaylightCalculator();

    @Test
    void calculatesAFullDaylightOutingLocally() {
        Instant start = Instant.parse("2026-06-21T08:00:00Z");
        Instant end = Instant.parse("2026-06-21T10:00:00Z");

        DaylightWindow result = calculator.calculate(
                PARIS,
                start,
                end,
                PARIS_TIME);

        assertThat(result.condition())
                .isEqualTo(DaylightCondition.NORMAL);
        assertThat(result.sunrise()).isBefore(result.sunset());
        assertThat(result.civilDawn()).isBefore(result.sunrise());
        assertThat(result.civilDusk()).isAfter(result.sunset());
        assertThat(result.expectedDaylightMinutes()).isEqualTo(120);
        assertThat(result.expectedDarknessMinutes()).isZero();
    }

    @Test
    void reportsTheExpectedMinutesOutsideCivilTwilight() {
        Instant start = Instant.parse("2026-06-21T22:00:00Z");
        Instant end = start.plus(Duration.ofMinutes(75));

        DaylightWindow result = calculator.calculate(
                PARIS,
                start,
                end,
                PARIS_TIME);

        assertThat(result.expectedDaylightMinutes()).isZero();
        assertThat(result.expectedDarknessMinutes()).isEqualTo(75);
    }
}
