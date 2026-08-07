package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.LocationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Redis adapter for {@link LocationCache}. Stores locations as JSON strings
 * under string keys with a TTL (Redis {@code SET … EX …}).
 *
 * <p>Same JSON-on-strings pattern as {@link RedisWeatherCache} so the two
 * cache types coexist cleanly in a single Redis instance.
 */
@Component
public class RedisLocationCache implements LocationCache {

    private static final Logger log = LoggerFactory.getLogger(RedisLocationCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisLocationCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Optional<Location> get(String key) {
        Objects.requireNonNull(key, "key");
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(mapper.readValue(json, Location.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached location at key '{}': {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for location key '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Location location, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            String json = mapper.writeValueAsString(location);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize location for key '{}': {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis SET failed for location key '{}': {}", key, e.getMessage());
        }
    }
}