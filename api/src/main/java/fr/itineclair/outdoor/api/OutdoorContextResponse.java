package fr.itineclair.outdoor.api;

import java.time.Instant;
import java.time.LocalDateTime;

import fr.itineclair.outdoor.OutdoorContextView;

public record OutdoorContextResponse(
        boolean planned,
        LocalDateTime plannedStartLocal,
        Instant plannedStartAt,
        Instant plannedEndAt,
        Integer plannedDurationMinutes,
        String timeZone,
        Instant updatedAt,
        DaylightResponse daylight,
        WeatherResponse weather) {

    static OutdoorContextResponse notPlanned() {
        return new OutdoorContextResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    static OutdoorContextResponse from(OutdoorContextView context) {
        return new OutdoorContextResponse(
                true,
                context.plannedStartLocal(),
                context.plannedStartAt(),
                context.plannedEndAt(),
                context.plannedDurationMinutes(),
                context.timeZone(),
                context.updatedAt(),
                DaylightResponse.from(context.daylight()),
                WeatherResponse.from(context.weather()));
    }
}
