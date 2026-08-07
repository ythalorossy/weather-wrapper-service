package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;

import java.time.Duration;
import java.util.Optional;

/**
 * Port (driving adapter contract): cache for geocoding results
 * (city name → {@link Location}).
 *
 * <p>Implementation lives in the infrastructure layer (Redis). The domain and
 * application layers depend only on this interface.
 *
 * <p>Note: only positive results are cached. A miss in this cache does NOT
 * imply the city does not exist — it just means we don't have a stored
 * answer. Negative caching (remembering "this city doesn't exist") is a
 * future enhancement.
 */
public interface LocationCache {

    /**
     * Retrieves a cached location by key.
     *
     * @param key the cache key (e.g., {@code "geo:arlington, va"}); must not be null
     * @return the cached location, or {@link Optional#empty()} on miss
     */
    Optional<Location> get(String key);

    /**
     * Stores a location under the given key with a TTL. Replaces any existing value.
     *
     * @param key      the cache key; must not be null
     * @param location the location to cache; must not be null
     * @param ttl      time-to-live; must be positive
     */
    void put(String key, Location location, Duration ttl);
}