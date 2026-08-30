package fr.itineclair.outdoor;

public class ForecastOutsideHorizonException
        extends WeatherProviderException {

    public ForecastOutsideHorizonException() {
        super("The planned outing is outside the provider forecast horizon.");
    }
}
