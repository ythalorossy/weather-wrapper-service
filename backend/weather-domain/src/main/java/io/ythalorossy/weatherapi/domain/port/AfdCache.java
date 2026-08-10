package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.AfdProduct;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Cache-aside cache for Area Forecast Discussion products. Keys are
 * formed by the application layer (e.g., {@code afd:{officeId}}) so the
 * cache is reusable across providers.
 */
public interface AfdCache {

    Optional<AfdProduct> get(String key);

    void put(String key, AfdProduct value, Duration ttl);

    default void requireValidKey(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }
}
