package fr.itineclair.outdoor.weather;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "itineclair.weather.open-meteo")
public record OpenMeteoProperties(
        boolean enabled,
        URI baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout,
        Duration forecastHorizon) {

    public OpenMeteoProperties {
        Objects.requireNonNull(baseUrl, "base-url is required.");
        apiKey = apiKey == null ? "" : apiKey.strip();
        requirePositive(connectTimeout, "connect-timeout");
        requirePositive(readTimeout, "read-timeout");
        requirePositive(forecastHorizon, "forecast-horizon");
        validateBaseUrl(baseUrl);

        if (enabled
                && "customer-api.open-meteo.com"
                .equalsIgnoreCase(baseUrl.getHost())
                && apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "api-key is required for the Open-Meteo customer endpoint.");
        }
    }

    private static void validateBaseUrl(URI baseUrl) {
        String scheme = baseUrl.getScheme();
        boolean localHttp = "http".equalsIgnoreCase(scheme)
                && ("localhost".equalsIgnoreCase(baseUrl.getHost())
                || "127.0.0.1".equals(baseUrl.getHost()));

        if (!"https".equalsIgnoreCase(scheme) && !localHttp) {
            throw new IllegalArgumentException(
                    "base-url must use HTTPS outside local tests.");
        }

        if (baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null) {
            throw new IllegalArgumentException(
                    "base-url must be an absolute provider URL without credentials or query string.");
        }
    }

    private static void requirePositive(
            Duration value,
            String property) {
        Objects.requireNonNull(value, property + " is required.");

        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    property + " must be greater than zero.");
        }
    }
}
