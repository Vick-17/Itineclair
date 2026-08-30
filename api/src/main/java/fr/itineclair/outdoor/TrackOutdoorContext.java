package fr.itineclair.outdoor;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "track_outdoor_contexts")
class TrackOutdoorContext {

    @Id
    @Column(name = "track_id")
    private UUID trackId;

    @Column(name = "planned_start_at", nullable = false)
    private Instant plannedStartAt;

    @Column(name = "planned_duration_minutes", nullable = false)
    private int plannedDurationMinutes;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "weather_consent_at")
    private Instant weatherConsentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_status", nullable = false, length = 32)
    private WeatherStatus weatherStatus;

    @Column(name = "weather_source", length = 80)
    private String weatherSource;

    @Column(name = "weather_attribution_url", length = 255)
    private String weatherAttributionUrl;

    @Column(name = "weather_checked_at")
    private Instant weatherCheckedAt;

    @Column(name = "weather_valid_from")
    private Instant weatherValidFrom;

    @Column(name = "weather_valid_until")
    private Instant weatherValidUntil;

    @Column(name = "weather_minimum_temperature_celsius")
    private Double weatherMinimumTemperatureCelsius;

    @Column(name = "weather_maximum_temperature_celsius")
    private Double weatherMaximumTemperatureCelsius;

    @Column(name = "weather_minimum_apparent_celsius")
    private Double weatherMinimumApparentCelsius;

    @Column(name = "weather_maximum_apparent_celsius")
    private Double weatherMaximumApparentCelsius;

    @Column(name = "weather_maximum_precipitation_percent")
    private Integer weatherMaximumPrecipitationPercent;

    @Column(name = "weather_precipitation_sum_mm")
    private Double weatherPrecipitationSumMillimeters;

    @Column(name = "weather_snowfall_sum_cm")
    private Double weatherSnowfallSumCentimeters;

    @Column(name = "weather_maximum_wind_speed_kmh")
    private Double weatherMaximumWindSpeedKilometersPerHour;

    @Column(name = "weather_maximum_wind_gust_kmh")
    private Double weatherMaximumWindGustKilometersPerHour;

    @Column(name = "weather_model_elevation_meters")
    private Double weatherModelElevationMeters;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TrackOutdoorContext() {
        // Constructeur requis par JPA.
    }

    private TrackOutdoorContext(
            UUID trackId,
            Instant plannedStartAt,
            int plannedDurationMinutes,
            String timeZone,
            Instant updatedAt) {
        this.trackId = trackId;
        updatePlan(
                plannedStartAt,
                plannedDurationMinutes,
                timeZone,
                updatedAt);
        clearWeather();
    }

    static TrackOutdoorContext create(
            UUID trackId,
            Instant plannedStartAt,
            int plannedDurationMinutes,
            String timeZone,
            Instant updatedAt) {
        return new TrackOutdoorContext(
                trackId,
                plannedStartAt,
                plannedDurationMinutes,
                timeZone,
                updatedAt);
    }

    void updatePlan(
            Instant plannedStartAt,
            int plannedDurationMinutes,
            String timeZone,
            Instant updatedAt) {
        this.plannedStartAt = plannedStartAt;
        this.plannedDurationMinutes = plannedDurationMinutes;
        this.timeZone = timeZone;
        this.updatedAt = updatedAt;
    }

    void clearWeather() {
        weatherConsentAt = null;
        weatherStatus = WeatherStatus.NOT_REQUESTED;
        weatherSource = null;
        weatherAttributionUrl = null;
        weatherCheckedAt = null;
        clearWeatherValues();
    }

    void recordWeatherFailure(
            WeatherStatus status,
            String source,
            String attributionUrl,
            Instant consentAt,
            Instant checkedAt) {
        if (status != WeatherStatus.UNAVAILABLE
                && status != WeatherStatus.OUTSIDE_FORECAST_HORIZON) {
            throw new IllegalArgumentException(
                    "A failed weather lookup requires a failure status.");
        }

        weatherConsentAt = consentAt;
        weatherStatus = status;
        weatherSource = source;
        weatherAttributionUrl = attributionUrl;
        weatherCheckedAt = checkedAt;
        clearWeatherValues();
    }

    void recordWeather(
            WeatherForecast forecast,
            Instant consentAt) {
        weatherConsentAt = consentAt;
        weatherStatus = WeatherStatus.AVAILABLE;
        weatherSource = forecast.source();
        weatherAttributionUrl = forecast.attributionUrl();
        weatherCheckedAt = forecast.retrievedAt();
        weatherValidFrom = forecast.validFrom();
        weatherValidUntil = forecast.validUntil();
        weatherMinimumTemperatureCelsius =
                forecast.minimumTemperatureCelsius();
        weatherMaximumTemperatureCelsius =
                forecast.maximumTemperatureCelsius();
        weatherMinimumApparentCelsius =
                forecast.minimumApparentCelsius();
        weatherMaximumApparentCelsius =
                forecast.maximumApparentCelsius();
        weatherMaximumPrecipitationPercent =
                forecast.maximumPrecipitationProbabilityPercent();
        weatherPrecipitationSumMillimeters =
                forecast.precipitationSumMillimeters();
        weatherSnowfallSumCentimeters =
                forecast.snowfallSumCentimeters();
        weatherMaximumWindSpeedKilometersPerHour =
                forecast.maximumWindSpeedKilometersPerHour();
        weatherMaximumWindGustKilometersPerHour =
                forecast.maximumWindGustKilometersPerHour();
        weatherModelElevationMeters =
                forecast.modelElevationMeters();
    }

    private void clearWeatherValues() {
        weatherValidFrom = null;
        weatherValidUntil = null;
        weatherMinimumTemperatureCelsius = null;
        weatherMaximumTemperatureCelsius = null;
        weatherMinimumApparentCelsius = null;
        weatherMaximumApparentCelsius = null;
        weatherMaximumPrecipitationPercent = null;
        weatherPrecipitationSumMillimeters = null;
        weatherSnowfallSumCentimeters = null;
        weatherMaximumWindSpeedKilometersPerHour = null;
        weatherMaximumWindGustKilometersPerHour = null;
        weatherModelElevationMeters = null;
    }

    UUID trackId() {
        return trackId;
    }

    Instant plannedStartAt() {
        return plannedStartAt;
    }

    int plannedDurationMinutes() {
        return plannedDurationMinutes;
    }

    String timeZone() {
        return timeZone;
    }

    Instant weatherConsentAt() {
        return weatherConsentAt;
    }

    WeatherStatus weatherStatus() {
        return weatherStatus;
    }

    String weatherSource() {
        return weatherSource;
    }

    String weatherAttributionUrl() {
        return weatherAttributionUrl;
    }

    Instant weatherCheckedAt() {
        return weatherCheckedAt;
    }

    Instant weatherValidFrom() {
        return weatherValidFrom;
    }

    Instant weatherValidUntil() {
        return weatherValidUntil;
    }

    Double weatherMinimumTemperatureCelsius() {
        return weatherMinimumTemperatureCelsius;
    }

    Double weatherMaximumTemperatureCelsius() {
        return weatherMaximumTemperatureCelsius;
    }

    Double weatherMinimumApparentCelsius() {
        return weatherMinimumApparentCelsius;
    }

    Double weatherMaximumApparentCelsius() {
        return weatherMaximumApparentCelsius;
    }

    Integer weatherMaximumPrecipitationPercent() {
        return weatherMaximumPrecipitationPercent;
    }

    Double weatherPrecipitationSumMillimeters() {
        return weatherPrecipitationSumMillimeters;
    }

    Double weatherSnowfallSumCentimeters() {
        return weatherSnowfallSumCentimeters;
    }

    Double weatherMaximumWindSpeedKilometersPerHour() {
        return weatherMaximumWindSpeedKilometersPerHour;
    }

    Double weatherMaximumWindGustKilometersPerHour() {
        return weatherMaximumWindGustKilometersPerHour;
    }

    Double weatherModelElevationMeters() {
        return weatherModelElevationMeters;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
