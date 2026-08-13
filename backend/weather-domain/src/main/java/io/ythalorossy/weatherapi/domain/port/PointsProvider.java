package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Gridpoint;
import io.ythalorossy.weatherapi.domain.model.Location;

/**
 * Outbound port for resolving a {@link Location} to the NWS gridpoint
 * triple required for gridpoint-keyed NWS endpoints
 * ({@code /gridpoints/{gridId}/{x},{y}/...}). Sourced from the NWS
 * {@code /points/{lat},{lon}} response.
 *
 * <p>Implementations are expected to perform any caching/memoization
 * they need; the application layer does not wrap calls.
 */
public interface PointsProvider {

    /**
     * @param location resolved location; must not be null
     * @return the NWS gridpoint triple for {@code location}; never null
     * @throws io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException
     *         if the upstream lookup fails or returns an incomplete body
     */
    Gridpoint getGridpoint(Location location);
}
