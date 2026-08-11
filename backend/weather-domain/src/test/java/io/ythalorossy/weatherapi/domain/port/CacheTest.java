package io.ythalorossy.weatherapi.domain.port;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class CacheTest {

    @Test
    void genericPortExposesGetAndPut() {
        // Compile-time assertion: a Cache<String> has get(String) returning
        // Optional<String> and put(String, String, Duration).
        Cache<String> cache = new Cache<>() {
            @Override public Optional<String> get(String key) { return Optional.of(key); }
            @Override public void put(String key, String value, Duration ttl) {}
        };
        assertThat(cache.get("k")).contains("k");
    }
}
