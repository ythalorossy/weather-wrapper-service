package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.WeatherForecast;

import java.time.Duration;
import java.util.Optional;

/**
 * Port (driving adapter contract): cache for weather forecasts.
 *
 * <p>Implementation lives in the infrastructure layer (Redis). The domain and application
 * layers depend only on this interface.
 */
public interface WeatherCache {

    /**
     * Retrieves a cached forecast by key.
     *
     * @param key the cache key; must not be null
     * @return the cached forecast, or {@link Optional#empty()} on miss
     */
    Optional<WeatherForecast> get(String key);

    /**
     * Stores a forecast under the given key with a TTL. Replaces any existing value.
     *
     * @param key      the cache key; must not be null
     * @param forecast the forecast to cache; must not be null
     * @param ttl      time-to-live; must be positive
     */
    void put(String key, WeatherForecast forecast, Duration ttl);
}