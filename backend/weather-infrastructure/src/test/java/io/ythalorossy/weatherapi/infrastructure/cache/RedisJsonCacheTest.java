package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class RedisJsonCacheTest {

    static GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
    static RedisJsonCache<Location> cache;
    static RedisJsonCache<List<WeatherAlert>> alertsCache;

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        REDIS.start();
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }

    @BeforeAll
    static void setup(@Autowired StringRedisTemplate template,
                      @Autowired ObjectMapper mapper) {
        cache = new RedisJsonCache<>(
                template, mapper, "geo",
                Location.class, new SimpleMeterRegistry());
        alertsCache = new RedisJsonCache<List<WeatherAlert>>(
                template, mapper, "alerts",
                (Class<List<WeatherAlert>>) (Class) List.class, new SimpleMeterRegistry());
    }

    @AfterAll
    static void teardown() { REDIS.stop(); }

    @Test
    void roundTripsValueThroughRedis() {
        Location value = new Location(38.88, -77.09, "Arlington, VA");
        cache.put("arlington-va", value, Duration.ofMinutes(1));

        Optional<Location> hit = cache.get("arlington-va");
        assertThat(hit).isPresent();
        assertThat(hit.get().latitude()).isEqualTo(38.88);
        assertThat(hit.get().displayName()).isEqualTo("Arlington, VA");
    }

    @Test
    void missingKeyReturnsEmpty() {
        Optional<Location> miss = cache.get("does-not-exist");
        assertThat(miss).isEmpty();
    }

    @Test
    void roundTripsListOfAlertsAsTypedElements() {
        WeatherAlert alert = new WeatherAlert(
                "urn:oid:2.49.0.1.test",
                "Severe Thunderstorm Warning",
                AlertSeverity.Severe,
                AlertCertainty.Likely,
                AlertUrgency.Expected,
                AlertCategory.Met,
                "Severe Thunderstorm Warning issued ...",
                "Long description...",
                "Take shelter.",
                "Arlington County",
                Instant.parse("2026-08-08T21:53:00Z"),
                Instant.parse("2026-08-08T21:53:00Z"),
                Instant.parse("2026-08-08T22:30:00Z"),
                "https://example.com"
        );
        List<WeatherAlert> value = List.of(alert);
        alertsCache.put("arlington-va", value, Duration.ofMinutes(1));

        Optional<List<WeatherAlert>> hit = alertsCache.get("arlington-va");
        assertThat(hit).isPresent();
        List<WeatherAlert> alerts = hit.get();
        assertThat(alerts).hasSize(1);
        WeatherAlert roundTripped = alerts.get(0);
        assertThat(roundTripped).isInstanceOf(WeatherAlert.class);
        assertThat(roundTripped.event()).isEqualTo("Severe Thunderstorm Warning");
        assertThat(roundTripped.severity()).isEqualTo(AlertSeverity.Severe);
        assertThat(roundTripped.areaDesc()).isEqualTo("Arlington County");
    }
}