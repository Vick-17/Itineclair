package fr.itineclair.outdoor.weather;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fr.itineclair.outdoor.ForecastOutsideHorizonException;
import fr.itineclair.outdoor.WeatherForecast;
import fr.itineclair.outdoor.WeatherForecastProvider;
import fr.itineclair.outdoor.WeatherForecastRequest;
import fr.itineclair.outdoor.WeatherProviderException;

@Component
public class OpenMeteoWeatherForecastProvider
        implements WeatherForecastProvider {

    private static final String SOURCE_NAME = "Open-Meteo";
    private static final String ATTRIBUTION_URL =
            "https://open-meteo.com/";
    private static final String HOURLY_VARIABLES = String.join(",",
            "temperature_2m",
            "apparent_temperature",
            "precipitation_probability",
            "precipitation",
            "snowfall",
            "wind_speed_10m",
            "wind_gusts_10m");

    private final RestClient restClient;
    private final OpenMeteoProperties properties;
    private final Clock clock;

    public OpenMeteoWeatherForecastProvider(
            RestClient openMeteoRestClient,
            OpenMeteoProperties properties,
            Clock clock) {
        this.restClient = openMeteoRestClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String attributionUrl() {
        return ATTRIBUTION_URL;
    }

    @Override
    public WeatherForecast forecast(
            WeatherForecastRequest request) {
        Objects.requireNonNull(request, "request");

        if (!properties.enabled()) {
            throw new WeatherProviderException(
                    "The weather provider is disabled.");
        }

        Instant horizon = clock.instant()
                .plus(properties.forecastHorizon());

        if (request.plannedEndAt().isAfter(horizon)) {
            throw new ForecastOutsideHorizonException();
        }

        Instant requestedFrom = request.plannedStartAt()
                .truncatedTo(ChronoUnit.HOURS);
        Instant requestedUntil = ceilToHour(request.plannedEndAt());
        LocalDate startDate = requestedFrom
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        LocalDate endDate = requestedUntil
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        OpenMeteoResponse response;

        try {
            response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder
                                .path("/v1/forecast")
                                .queryParam("latitude", request.latitude())
                                .queryParam("longitude", request.longitude())
                                .queryParam("hourly", HOURLY_VARIABLES)
                                .queryParam("timezone", "GMT")
                                .queryParam("start_date", startDate)
                                .queryParam("end_date", endDate);

                        if (!properties.apiKey().isBlank()) {
                            uriBuilder.queryParam(
                                    "apikey",
                                    properties.apiKey());
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(OpenMeteoResponse.class);
        } catch (RuntimeException exception) {
            throw new WeatherProviderException(
                    "The weather provider request failed.",
                    exception);
        }

        return aggregate(
                response,
                requestedFrom,
                requestedUntil,
                clock.instant());
    }

    private WeatherForecast aggregate(
            OpenMeteoResponse response,
            Instant requestedFrom,
            Instant requestedUntil,
            Instant retrievedAt) {
        if (response == null
                || response.hourly() == null
                || response.hourly().time() == null) {
            throw new WeatherProviderException(
                    "The weather provider returned no hourly forecast.");
        }

        OpenMeteoHourly hourly = response.hourly();
        List<Integer> selectedIndexes = new ArrayList<>();
        List<Instant> selectedTimes = new ArrayList<>();

        for (int index = 0; index < hourly.time().size(); index++) {
            Instant time = parseUtcTime(hourly.time().get(index));

            if (!time.isBefore(requestedFrom)
                    && time.isBefore(requestedUntil)) {
                selectedIndexes.add(index);
                selectedTimes.add(time);
            }
        }

        if (selectedIndexes.isEmpty()) {
            throw new WeatherProviderException(
                    "The weather provider returned no data for the planned window.");
        }

        return new WeatherForecast(
                SOURCE_NAME,
                ATTRIBUTION_URL,
                retrievedAt,
                selectedTimes.getFirst(),
                selectedTimes.getLast().plus(1, ChronoUnit.HOURS),
                minimum(hourly.temperature_2m(), selectedIndexes),
                maximum(hourly.temperature_2m(), selectedIndexes),
                minimum(hourly.apparent_temperature(), selectedIndexes),
                maximum(hourly.apparent_temperature(), selectedIndexes),
                maximumInteger(
                        hourly.precipitation_probability(),
                        selectedIndexes),
                sum(hourly.precipitation(), selectedIndexes),
                sum(hourly.snowfall(), selectedIndexes),
                maximum(hourly.wind_speed_10m(), selectedIndexes),
                maximum(hourly.wind_gusts_10m(), selectedIndexes),
                finiteOrNull(response.elevation()));
    }

    private Instant parseUtcTime(String value) {
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new WeatherProviderException(
                    "The weather provider returned an invalid timestamp.",
                    exception);
        }
    }

    private Double minimum(
            List<Double> values,
            List<Integer> indexes) {
        return extrema(values, indexes, stream -> stream.min(Double::compare));
    }

    private Double maximum(
            List<Double> values,
            List<Integer> indexes) {
        return extrema(values, indexes, stream -> stream.max(Double::compare));
    }

    private Double extrema(
            List<Double> values,
            List<Integer> indexes,
            Function<java.util.stream.Stream<Double>,
                    java.util.Optional<Double>> operation) {
        if (values == null) {
            return null;
        }

        return operation.apply(indexes.stream()
                        .filter(index -> index < values.size())
                        .map(values::get)
                        .filter(Objects::nonNull)
                        .filter(Double::isFinite))
                .orElse(null);
    }

    private Integer maximumInteger(
            List<Integer> values,
            List<Integer> indexes) {
        if (values == null) {
            return null;
        }

        return indexes.stream()
                .filter(index -> index < values.size())
                .map(values::get)
                .filter(Objects::nonNull)
                .filter(value -> value >= 0 && value <= 100)
                .max(Integer::compare)
                .orElse(null);
    }

    private Double sum(
            List<Double> values,
            List<Integer> indexes) {
        if (values == null) {
            return null;
        }

        List<Double> selected = indexes.stream()
                .filter(index -> index < values.size())
                .map(values::get)
                .filter(Objects::nonNull)
                .filter(Double::isFinite)
                .filter(value -> value >= 0.0)
                .toList();

        if (selected.isEmpty()) {
            return null;
        }

        return selected.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private Double finiteOrNull(Double value) {
        return value != null && Double.isFinite(value)
                ? value
                : null;
    }

    private Instant ceilToHour(Instant value) {
        Instant truncated = value.truncatedTo(ChronoUnit.HOURS);
        return truncated.equals(value)
                ? value
                : truncated.plus(1, ChronoUnit.HOURS);
    }
}

record OpenMeteoResponse(
        Double elevation,
        OpenMeteoHourly hourly) {
}

record OpenMeteoHourly(
        List<String> time,
        List<Double> temperature_2m,
        List<Double> apparent_temperature,
        List<Integer> precipitation_probability,
        List<Double> precipitation,
        List<Double> snowfall,
        List<Double> wind_speed_10m,
        List<Double> wind_gusts_10m) {
}
