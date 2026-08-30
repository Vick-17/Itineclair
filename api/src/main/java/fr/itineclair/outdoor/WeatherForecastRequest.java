package fr.itineclair.outdoor;

import java.time.Instant;

public record WeatherForecastRequest(
        double latitude,
        double longitude,
        Instant plannedStartAt,
        Instant plannedEndAt) {
}
