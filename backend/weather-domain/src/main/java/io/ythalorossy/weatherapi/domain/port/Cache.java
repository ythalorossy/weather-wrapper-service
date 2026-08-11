package io.ythalorossy.weatherapi.domain.port;

import java.time.Duration;
import java.util.Optional;

public interface Cache<V> {
    Optional<V> get(String key);
    void put(String key, V value, Duration ttl);

    /**
     * Optional negative-cache hook. Returns true if the key has been explicitly
     * marked absent (a prior load returned empty). Default no-op for caches
     * that don't track negative results.
     */
    default boolean isAbsent(String key) {
        return false;
    }

    /**
     * Optional negative-cache hook. Records that a key has no value, so future
     * lookups can short-circuit via {@link #isAbsent(String)}. Default no-op.
     */
    default void markAbsent(String key, Duration ttl) {
        // no-op
    }
}

