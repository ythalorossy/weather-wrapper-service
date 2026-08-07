package io.ythalorossy.weatherapi.domain.exception;

/**
 * Thrown when the upstream weather provider is unreachable or returns a non-success
 * response. Distinguishes upstream failures from client errors.
 *
 * <p>Maps to HTTP 502 (Bad Gateway) in the API layer.
 */
public class WeatherProviderUnavailableException extends RuntimeException {

    public WeatherProviderUnavailableException(String message) {
        super(message);
    }

    public WeatherProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}