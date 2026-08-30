package fr.itineclair.outdoor;

import java.time.Instant;

public record WeatherForecast(
        String source,
        String attributionUrl,
        Instant retrievedAt,
        Instant validFrom,
        Instant validUntil,
        Double minimumTemperatureCelsius,
        Double maximumTemperatureCelsius,
        Double minimumApparentCelsius,
        Double maximumApparentCelsius,
        Integer maximumPrecipitationProbabilityPercent,
        Double precipitationSumMillimeters,
        Double snowfallSumCentimeters,
        Double maximumWindSpeedKilometersPerHour,
        Double maximumWindGustKilometersPerHour,
        Double modelElevationMeters) {
}
