package fr.itineclair.outdoor.weather;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import fr.itineclair.outdoor.ForecastOutsideHorizonException;
import fr.itineclair.outdoor.WeatherForecast;
import fr.itineclair.outdoor.WeatherForecastRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoWeatherForecastProviderTest {

    private static final Instant NOW =
            Instant.parse("2026-08-30T10:00:00Z");

    @Test
    void requestsAndAggregatesOnlyThePlannedHourlyWindow() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.open-meteo.test");
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .build();
        OpenMeteoWeatherForecastProvider provider = provider(
                builder.build(),
                Duration.ofDays(16));

        server.expect(requestTo(containsString("/v1/forecast")))
                .andExpect(requestTo(containsString(
                        "latitude=45.8326")))
                .andExpect(requestTo(containsString(
                        "timezone=GMT")))
                .andRespond(withSuccess("""
                        {
                          "elevation": 1210.0,
                          "hourly": {
                            "time": [
                              "2026-08-31T05:00",
                              "2026-08-31T06:00",
                              "2026-08-31T07:00",
                              "2026-08-31T08:00",
                              "2026-08-31T09:00",
                              "2026-08-31T10:00"
                            ],
                            "temperature_2m": [4.0, 5.0, 8.0, 12.0, 10.0, 9.0],
                            "apparent_temperature": [1.0, 2.0, 5.0, 10.0, 8.0, 7.0],
                            "precipitation_probability": [10, 20, 70, 40, 30, 10],
                            "precipitation": [0.0, 0.2, 1.0, 2.0, 0.0, 0.0],
                            "snowfall": [0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                            "wind_speed_10m": [10.0, 12.0, 32.0, 25.0, 18.0, 14.0],
                            "wind_gusts_10m": [20.0, 22.0, 58.0, 46.0, 35.0, 28.0]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        WeatherForecast result = provider.forecast(
                new WeatherForecastRequest(
                        45.8326,
                        6.8652,
                        Instant.parse("2026-08-31T06:30:00Z"),
                        Instant.parse("2026-08-31T08:15:00Z")));

        assertThat(result.validFrom())
                .isEqualTo("2026-08-31T06:00:00Z");
        assertThat(result.validUntil())
                .isEqualTo("2026-08-31T09:00:00Z");
        assertThat(result.minimumTemperatureCelsius()).isEqualTo(5.0);
        assertThat(result.maximumTemperatureCelsius()).isEqualTo(12.0);
        assertThat(result.maximumPrecipitationProbabilityPercent())
                .isEqualTo(70);
        assertThat(result.precipitationSumMillimeters()).isEqualTo(3.2);
        assertThat(result.maximumWindGustKilometersPerHour())
                .isEqualTo(58.0);
        assertThat(result.modelElevationMeters()).isEqualTo(1_210.0);
        server.verify();
    }

    @Test
    void doesNotCallProviderOutsideConfiguredHorizon() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.open-meteo.test");
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .build();
        OpenMeteoWeatherForecastProvider provider = provider(
                builder.build(),
                Duration.ofDays(2));

        assertThatThrownBy(() -> provider.forecast(
                new WeatherForecastRequest(
                        45.8326,
                        6.8652,
                        NOW.plus(Duration.ofDays(3)),
                        NOW.plus(Duration.ofDays(3)).plusSeconds(3600))))
                .isInstanceOf(ForecastOutsideHorizonException.class);

        server.verify();
    }

    private OpenMeteoWeatherForecastProvider provider(
            RestClient restClient,
            Duration horizon) {
        OpenMeteoProperties properties = new OpenMeteoProperties(
                true,
                URI.create("https://api.open-meteo.test"),
                "",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                horizon);

        return new OpenMeteoWeatherForecastProvider(
                restClient,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
