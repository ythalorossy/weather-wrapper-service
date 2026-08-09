package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
@Testcontainers
class RedisObservationCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisObservationCache cache;

    private static final String KEY = "obs:38.88,-77.09";
    private final Observation obs = new Observation(
            "KIAD", "Washington/Dulles International Airport, VA",
            Instant.parse("2026-08-08T21:35:00Z"),
            78.4, 71.6, 5.2, 315, "NW", 83.5, 30.02, "Cloudy"
    );

    @Test
    void putThenGetRoundtripsValue() {
        cache.put(KEY, obs, Duration.ofMinutes(1));

        Optional<Observation> got = cache.get(KEY);

        assertThat(got).isPresent();
        assertThat(got.get().stationId()).isEqualTo("KIAD");
        assertThat(got.get().temperatureFahrenheit()).isEqualTo(78.4);
        assertThat(got.get().textDescription()).isEqualTo("Cloudy");
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("obs:nonexistent")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("obs:expiring", obs, Duration.ofMillis(500));
        assertThat(cache.get("obs:expiring")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("obs:expiring")).isEmpty();
    }
}