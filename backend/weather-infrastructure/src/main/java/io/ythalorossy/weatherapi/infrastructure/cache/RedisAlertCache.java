package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.port.AlertCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Redis adapter for {@link AlertCache}. Keys live under the {@code alerts:*}
 * namespace. 5-minute TTL by configuration (alerts change fast \u2014 a stale
 * "Tornado warning" is a worse UX than a fresh fetch).
 */
@Component
public class RedisAlertCache implements AlertCache {

    private static final Logger log = LoggerFactory.getLogger(RedisAlertCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisAlertCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Optional<List<WeatherAlert>> get(String key) {
        Objects.requireNonNull(key, "key");
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, WeatherAlert.class)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize alerts at key '{}': {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for alerts key '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, List<WeatherAlert> alerts, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(alerts, "alerts");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            String json = mapper.writeValueAsString(alerts);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize alerts for key '{}': {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis SET failed for alerts key '{}': {}", key, e.getMessage());
        }
    }
}