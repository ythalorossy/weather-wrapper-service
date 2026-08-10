package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.SunTimes;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Cache-aside cache for sun times. Keys are formed by the application layer
 * (e.g., {@code sun:{lat:.2f},{lon:.2f}:{date-ISO}}) so that two locations
 * that round to the same lat/lon share a slot for the same day.
 */
public interface SunTimesCache {

    Optional<SunTimes> get(String key);

    void put(String key, SunTimes value, Duration ttl);

    default void requireValidKey(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }
}
