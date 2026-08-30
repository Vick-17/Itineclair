package fr.itineclair.outdoor.api;

import java.time.Instant;

import fr.itineclair.outdoor.WeatherStatus;
import fr.itineclair.outdoor.WeatherSummary;

public record WeatherResponse(
        WeatherStatus status,
        String source,
        String attributionUrl,
        Instant checkedAt,
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

    static WeatherResponse from(WeatherSummary weather) {
        return new WeatherResponse(
                weather.status(),
                weather.source(),
                weather.attributionUrl(),
                weather.checkedAt(),
                weather.validFrom(),
                weather.validUntil(),
                weather.minimumTemperatureCelsius(),
                weather.maximumTemperatureCelsius(),
                weather.minimumApparentCelsius(),
                weather.maximumApparentCelsius(),
                weather.maximumPrecipitationProbabilityPercent(),
                weather.precipitationSumMillimeters(),
                weather.snowfallSumCentimeters(),
                weather.maximumWindSpeedKilometersPerHour(),
                weather.maximumWindGustKilometersPerHour(),
                weather.modelElevationMeters());
    }
}
