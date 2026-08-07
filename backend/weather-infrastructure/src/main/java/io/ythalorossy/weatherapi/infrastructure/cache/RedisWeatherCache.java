package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Redis adapter for {@link WeatherCache}. Stores forecasts as JSON strings
 * under string keys with a TTL (Redis {@code SET … EX …}).
 */
@Component
public class RedisWeatherCache implements WeatherCache {

    private static final Logger log = LoggerFactory.getLogger(RedisWeatherCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisWeatherCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Optional<WeatherForecast> get(String key) {
        Objects.requireNonNull(key, "key");
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(mapper.readValue(json, WeatherForecast.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached value at key '{}': {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for key '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, WeatherForecast forecast, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(forecast, "forecast");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            String json = mapper.writeValueAsString(forecast);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize forecast for key '{}': {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis SET failed for key '{}': {}", key, e.getMessage());
        }
    }
}