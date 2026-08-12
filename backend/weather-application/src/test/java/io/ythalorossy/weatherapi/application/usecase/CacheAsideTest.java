package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.port.Cache;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class CacheAsideTest {

    @Test
    void returnsCachedValueOnHit() {
        Cache<String> cache = new InMemoryCache();
        cache.put("k", "cached", Duration.ofMinutes(1));
        AtomicInteger calls = new AtomicInteger();

        String v = CacheAside.getOrLoad(cache, "k", Duration.ofMinutes(1),
                () -> { calls.incrementAndGet(); return "loaded"; });

        assertThat(v).isEqualTo("cached");
        assertThat(calls).hasValue(0);
    }

    @Test
    void loadsAndCachesOnMiss() {
        Cache<String> cache = new InMemoryCache();
        AtomicInteger calls = new AtomicInteger();

        String v = CacheAside.getOrLoad(cache, "k", Duration.ofMinutes(1),
                () -> { calls.incrementAndGet(); return "loaded"; });

        assertThat(v).isEqualTo("loaded");
        assertThat(calls).hasValue(1);
        assertThat(cache.get("k")).contains("loaded");
    }

    @Test
    void propagatesSupplierExceptions() {
        Cache<String> cache = new InMemoryCache();
        assertThatThrownBy(() -> CacheAside.getOrLoad(cache, "k",
                Duration.ofMinutes(1), () -> { throw new IllegalStateException("boom"); }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }

    static class InMemoryCache implements Cache<String> {
        private final java.util.Map<String, String> map = new java.util.HashMap<>();
        @Override public Optional<String> get(String key) { return Optional.ofNullable(map.get(key)); }
        @Override public void put(String key, String value, Duration ttl) { map.put(key, value); }
    }
}
