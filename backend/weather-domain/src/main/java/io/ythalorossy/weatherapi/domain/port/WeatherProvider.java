package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;

/**
 * Port (driven adapter contract): fetches a weather forecast for a given location.
 *
 * <p>Implementations live in the infrastructure layer (e.g., NWS). Domain and application
 * code depend only on this interface.
 */
public interface WeatherProvider {

    /**
     * Fetches the current forecast for the given location.
     *
     * @param location the location to query; must not be null
     * @return the forecast for that location; never null
     * @throws WeatherProviderUnavailableException if the upstream provider is unreachable
     *         or returns a non-success response
     */
    WeatherForecast getForecast(Location location);
}