package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
 *
 * <p>Exposes Micrometer counters under {@code weather.cache.weather} with
 * tag {@code result=hit|miss}. The hit-ratio is computed as
 * {@code hit / (hit + miss)}.
 */
@Component
public class RedisWeatherCache implements WeatherCache {

    private static final Logger log = LoggerFactory.getLogger(RedisWeatherCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Counter hitCounter;
    private final Counter missCounter;

    public RedisWeatherCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.mapper = mapper;
        this.hitCounter = Counter.builder("weather.cache.weather")
                .description("Weather cache lookups by outcome")
                .tag("result", "hit")
                .register(meterRegistry);
        this.missCounter = Counter.builder("weather.cache.weather")
                .description("Weather cache lookups by outcome")
                .tag("result", "miss")
                .register(meterRegistry);
    }

    @Override
    public Optional<WeatherForecast> get(String key) {
        Objects.requireNonNull(key, "key");
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                missCounter.increment();
                return Optional.empty();
            }
            hitCounter.increment();
            return Optional.of(mapper.readValue(json, WeatherForecast.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached value at key '{}': {}", key, e.getMessage());
            missCounter.increment();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for key '{}': {}", key, e.getMessage());
            missCounter.increment();
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
            log.warn("Redis SET failed for forecast key '{}': {}", key, e.getMessage());
        }
    }
}