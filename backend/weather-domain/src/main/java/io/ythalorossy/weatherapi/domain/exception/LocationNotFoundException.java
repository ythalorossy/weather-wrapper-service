package io.ythalorossy.weatherapi.domain.exception;

/**
 * Thrown when a city name cannot be resolved by any registered {@code GeocodingProvider}.
 *
 * <p>Maps to HTTP 404 in the API layer.
 */
public class LocationNotFoundException extends RuntimeException {

    private final String cityName;

    public LocationNotFoundException(String cityName) {
        super("Location not found for city: " + cityName);
        this.cityName = cityName;
    }

    public String cityName() {
        return cityName;
    }
}