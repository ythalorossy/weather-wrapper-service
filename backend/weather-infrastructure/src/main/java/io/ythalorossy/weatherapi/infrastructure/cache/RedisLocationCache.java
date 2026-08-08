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
 * Redis adapter for {@link LocationCache}. Stores positive locations as JSON
 * under {@code geo:*} keys and negative results ("no upstream match") under
 * {@code absent:geo:*} keys, both with TTLs (Redis {@code SET … EX …}).
 *
 * <p>The separate prefix means a negative entry never shadows or pollutes a
 * positive entry at the same logical key. {@link #get(String)} reads only
 * the positive namespace; {@link #isAbsent(String)} reads only the negative
 * namespace.
 *
 * <p>Same JSON-on-strings pattern as {@link RedisWeatherCache} so all cache
 * types coexist cleanly in a single Redis instance.
 */
@Component
public class RedisLocationCache implements LocationCache {

    private static final Logger log = LoggerFactory.getLogger(RedisLocationCache.class);

    /** Prefix added to keys for negative ("no match") entries. */
    static final String ABSENT_PREFIX = "absent:";

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

    @Override
    public boolean isAbsent(String key) {
        Objects.requireNonNull(key, "key");
        try {
            Boolean exists = redis.hasKey(ABSENT_PREFIX + key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            // Redis down: don't block the request; treat as not-cached-absent
            // so the caller falls through to the geocoder. Degraded but safe.
            log.warn("Redis EXISTS failed for absent key '{}': {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public void markAbsent(String key, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            redis.opsForValue().set(ABSENT_PREFIX + key, "1", ttl);
        } catch (Exception e) {
            log.warn("Redis SET failed for absent key '{}': {}", key, e.getMessage());
        }
    }
}