package fr.itineclair.outdoor;

public interface WeatherForecastProvider {

    String sourceName();

    String attributionUrl();

    WeatherForecast forecast(WeatherForecastRequest request);
}
