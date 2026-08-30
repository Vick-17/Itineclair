package fr.itineclair.outdoor.api;

import java.time.Instant;

import fr.itineclair.outdoor.DaylightCondition;
import fr.itineclair.outdoor.DaylightWindow;

public record DaylightResponse(
        Instant sunrise,
        Instant sunset,
        Instant civilDawn,
        Instant civilDusk,
        long expectedDaylightMinutes,
        long expectedDarknessMinutes,
        DaylightCondition condition) {

    static DaylightResponse from(DaylightWindow daylight) {
        return new DaylightResponse(
                daylight.sunrise(),
                daylight.sunset(),
                daylight.civilDawn(),
                daylight.civilDusk(),
                daylight.expectedDaylightMinutes(),
                daylight.expectedDarknessMinutes(),
                daylight.condition());
    }
}
