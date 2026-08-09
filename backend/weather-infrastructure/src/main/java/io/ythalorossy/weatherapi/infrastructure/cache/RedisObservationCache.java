package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.port.ObservationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Redis adapter for {@link ObservationCache}. Keys live under the {@code obs:*}
 * namespace. 10-minute TTL by configuration (observations update every
 * 5\u201315 min, so a slightly longer TTL lets the cache survive brief NWS blips).
 */
@Component
public class RedisObservationCache implements ObservationCache {

    private static final Logger log = LoggerFactory.getLogger(RedisObservationCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisObservationCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Optional<Observation> get(String key) {
        Objects.requireNonNull(key, "key");
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(mapper.readValue(json, Observation.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize observation at key '{}': {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for observation key '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Observation observation, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            String json = mapper.writeValueAsString(observation);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize observation for key '{}': {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis SET failed for observation key '{}': {}", key, e.getMessage());
        }
    }
}