package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;

import java.time.Duration;
import java.util.Optional;

/**
 * Port (driving adapter contract): cache for geocoding results
 * (city name → {@link Location}).
 *
 * <p>Stores both positive results ("this city resolves to these coordinates")
 * and negative results ("Nominatim has no match for this city"). Negative
 * entries have a separate (typically much shorter) TTL — see
 * {@link #markAbsent(String, Duration)}.
 *
 * <p>Implementation lives in the infrastructure layer (Redis). The domain and
 * application layers depend only on this interface.
 */
public interface LocationCache {

    /**
     * Retrieves a cached positive location by key.
     *
     * @param key the cache key (e.g., {@code "geo:arlington, va"}); must not be null
     * @return the cached location, or {@link Optional#empty()} on miss
     */
    Optional<Location> get(String key);

    /**
     * Stores a positive location under the given key with a TTL. Replaces any
     * existing positive value. Does NOT clear any negative entry at the same
     * key — those live in a separate prefix.
     *
     * @param key      the cache key; must not be null
     * @param location the location to cache; must not be null
     * @param ttl      time-to-live; must be positive
     */
    void put(String key, Location location, Duration ttl);

    /**
     * Returns {@code true} if the given key has been explicitly cached as a
     * negative result ("no match from the upstream provider") and has not yet
     * expired.
     *
     * <p>This is independent of {@link #get(String)}: a key with a stored
     * negative entry returns {@link Optional#empty()} from {@code get}.
     *
     * @param key the cache key; must not be null
     * @return true if a live negative entry exists for this key
     */
    boolean isAbsent(String key);

    /**
     * Caches a negative result for the given key with a short TTL. The next
     * caller that looks up the same key within the TTL will see
     * {@link #isAbsent(String)} return true and skip the upstream call.
     *
     * @param key the cache key; must not be null
     * @param ttl time-to-live; must be positive (typically short, e.g. 60 s)
     */
    void markAbsent(String key, Duration ttl);
}