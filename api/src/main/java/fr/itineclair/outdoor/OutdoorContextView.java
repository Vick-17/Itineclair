package fr.itineclair.outdoor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record OutdoorContextView(
        LocalDateTime plannedStartLocal,
        Instant plannedStartAt,
        Instant plannedEndAt,
        int plannedDurationMinutes,
        String timeZone,
        Instant updatedAt,
        DaylightWindow daylight,
        WeatherSummary weather) {

    static OutdoorContextView from(
            TrackOutdoorContext context,
            DaylightWindow daylight,
            WeatherForecastProvider weatherProvider) {
        Instant plannedEndAt = context.plannedStartAt()
                .plusSeconds(context.plannedDurationMinutes() * 60L);

        return new OutdoorContextView(
                LocalDateTime.ofInstant(
                        context.plannedStartAt(),
                        ZoneId.of(context.timeZone())),
                context.plannedStartAt(),
                plannedEndAt,
                context.plannedDurationMinutes(),
                context.timeZone(),
                context.updatedAt(),
                daylight,
                WeatherSummary.from(context, weatherProvider));
    }
}
