package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.port.Cache;
import java.time.Duration;
import java.util.function.Supplier;

public final class CacheAside {

    private CacheAside() {}

    public static <V> V getOrLoad(Cache<V> cache, String key, Duration ttl,
                                     Supplier<V> loader) {
        return cache.get(key).orElseGet(() -> {
            V value = loader.get();
            cache.put(key, value, ttl);
            return value;
        });
    }

    public static void requirePositive(Duration ttl, String name) {
        if (ttl.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + ttl);
        }
    }
}
