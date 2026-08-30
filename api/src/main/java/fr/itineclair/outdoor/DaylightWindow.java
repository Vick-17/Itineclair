package fr.itineclair.outdoor;

import java.time.Instant;

public record DaylightWindow(
        Instant sunrise,
        Instant sunset,
        Instant civilDawn,
        Instant civilDusk,
        long expectedDaylightMinutes,
        long expectedDarknessMinutes,
        DaylightCondition condition) {
}
