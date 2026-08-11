package io.ythalorossy.weatherapi.domain.port;

import java.time.Duration;
import java.util.Optional;

public interface Cache<V> {
    Optional<V> get(String key);
    void put(String key, V value, Duration ttl);
}
