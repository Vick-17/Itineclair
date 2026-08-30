package fr.itineclair.outdoor;

import java.time.Instant;

public record WeatherSummary(
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

    static WeatherSummary from(
            TrackOutdoorContext context,
            WeatherForecastProvider provider) {
        String source = context.weatherSource() == null
                ? provider.sourceName()
                : context.weatherSource();
        String attributionUrl = context.weatherAttributionUrl() == null
                ? provider.attributionUrl()
                : context.weatherAttributionUrl();

        return new WeatherSummary(
                context.weatherStatus(),
                source,
                attributionUrl,
                context.weatherCheckedAt(),
                context.weatherValidFrom(),
                context.weatherValidUntil(),
                context.weatherMinimumTemperatureCelsius(),
                context.weatherMaximumTemperatureCelsius(),
                context.weatherMinimumApparentCelsius(),
                context.weatherMaximumApparentCelsius(),
                context.weatherMaximumPrecipitationPercent(),
                context.weatherPrecipitationSumMillimeters(),
                context.weatherSnowfallSumCentimeters(),
                context.weatherMaximumWindSpeedKilometersPerHour(),
                context.weatherMaximumWindGustKilometersPerHour(),
                context.weatherModelElevationMeters());
    }
}
