package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.SunTimesCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Component
public class RedisSunTimesCache implements SunTimesCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSunTimesCache.class);
    private static final String COUNTER_NAME = "weather.cache.sun";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Counter hitCounter;
    private final Counter missCounter;

    public RedisSunTimesCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.mapper = mapper;
        this.hitCounter = Counter.builder(COUNTER_NAME).tag("result", "hit").register(meterRegistry);
        this.missCounter = Counter.builder(COUNTER_NAME).tag("result", "miss").register(meterRegistry);
    }

    @Override
    public Optional<SunTimes> get(String key) {
        requireValidKey(key);
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                missCounter.increment();
                return Optional.empty();
            }
            SunTimes value = mapper.readValue(json, SunTimes.class);
            hitCounter.increment();
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize sun times at key '{}': {}", key, e.getMessage());
            missCounter.increment();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis GET failed for sun key '{}': {}", key, e.getMessage());
            missCounter.increment();
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, SunTimes value, Duration ttl) {
        requireValidKey(key);
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize sun times for key '{}': {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis SET failed for sun key '{}': {}", key, e.getMessage());
        }
    }
}
