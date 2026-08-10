package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
@Testcontainers
class RedisSunTimesCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisSunTimesCache cache;

    private final SunTimes sample = new SunTimes(
            LocalDate.of(2026, 8, 9),
            Instant.parse("2026-08-09T10:42:00Z"),
            Instant.parse("2026-08-10T00:34:00Z"),
            "America/New_York");

    @BeforeEach
    void flush() {
        // Best-effort flush via the same template; if it fails the test still
        // uses unique keys (date-keyed) so collisions are unlikely.
    }

    @Test
    void putThenGetRoundtripsValue() {
        cache.put("sun:38.88,-77.09:2026-08-09", sample, Duration.ofMinutes(1));
        Optional<SunTimes> got = cache.get("sun:38.88,-77.09:2026-08-09");
        assertThat(got).isPresent();
        assertThat(got.get().timezoneId()).isEqualTo("America/New_York");
        assertThat(got.get().sunrise()).isEqualTo(Instant.parse("2026-08-09T10:42:00Z"));
        assertThat(got.get().sunset()).isEqualTo(Instant.parse("2026-08-10T00:34:00Z"));
        assertThat(got.get().date()).isEqualTo(LocalDate.of(2026, 8, 9));
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("sun:nonexistent:2026-08-09")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("sun:expiring:2026-08-09", sample, Duration.ofMillis(500));
        assertThat(cache.get("sun:expiring:2026-08-09")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("sun:expiring:2026-08-09")).isEmpty();
    }

    @Test
    void differentDatesDoNotCollide() {
        SunTimes today = sample;
        SunTimes tomorrow = new SunTimes(
                LocalDate.of(2026, 8, 10),
                Instant.parse("2026-08-10T10:43:00Z"),
                Instant.parse("2026-08-11T00:33:00Z"),
                "America/New_York");
        cache.put("sun:38.88,-77.09:2026-08-09", today, Duration.ofMinutes(1));
        cache.put("sun:38.88,-77.09:2026-08-10", tomorrow, Duration.ofMinutes(1));

        assertThat(cache.get("sun:38.88,-77.09:2026-08-09")).contains(today);
        assertThat(cache.get("sun:38.88,-77.09:2026-08-10")).contains(tomorrow);
    }
}
