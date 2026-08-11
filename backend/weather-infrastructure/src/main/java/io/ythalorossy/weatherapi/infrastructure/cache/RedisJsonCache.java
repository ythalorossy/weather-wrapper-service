package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.ythalorossy.weatherapi.domain.port.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class RedisJsonCache<V> implements Cache<V> {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final String prefix;
    private final Class<V> type;
    private final Counter hits;
    private final Counter misses;

    public RedisJsonCache(StringRedisTemplate redis, ObjectMapper mapper,
                          String prefix, Class<V> type, MeterRegistry meters) {
        this.redis = Objects.requireNonNull(redis);
        this.mapper = Objects.requireNonNull(mapper);
        this.prefix = Objects.requireNonNull(prefix);
        this.type = Objects.requireNonNull(type);
        this.hits = Counter.builder("weather.cache." + prefix + ".hits")
            .description("Cache hits for " + prefix + " namespace")
            .register(meters);
        this.misses = Counter.builder("weather.cache." + prefix + ".misses")
            .description("Cache misses for " + prefix + " namespace")
            .register(meters);
    }

    @Override
    public Optional<V> get(String key) {
        Objects.requireNonNull(key, "key");
        try {
            String json = redis.opsForValue().get(prefix + ":" + key);
            if (json == null) {
                misses.increment();
                return Optional.empty();
            }
            hits.increment();
            return Optional.of(mapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize {} cache key '{}': {}",
                     prefix, key, e.getMessage());
            misses.increment();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for {} cache key '{}': {}",
                     prefix, key, e.getMessage());
            misses.increment();
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, V value, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(prefix + ":" + key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize {} cache key '{}': {}",
                     prefix, key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis SET failed for {} cache key '{}': {}",
                     prefix, key, e.getMessage());
        }
    }
}
