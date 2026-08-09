package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.HourlyForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Temperature;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
@Testcontainers
class RedisHourlyForecastCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisHourlyForecastCache cache;

    private static final String KEY = "hourly:test:38.88,-77.09";
    private final HourlyForecast hourly = new HourlyForecast(
            List.of(new HourlyForecastPeriod(
                    Instant.parse("2026-08-08T19:00:00Z"),
                    Temperature.fahrenheit(85),
                    "5 mph", "NW", "Sunny", true)),
            Instant.now(),
            "National Weather Service (api.weather.gov)"
    );

    @Test
    void putThenGetRoundtripsValue() {
        cache.put(KEY, hourly, Duration.ofMinutes(1));

        Optional<HourlyForecast> got = cache.get(KEY);

        assertThat(got).isPresent();
        assertThat(got.get().source()).isEqualTo("National Weather Service (api.weather.gov)");
        assertThat(got.get().periods()).hasSize(1);
        assertThat(got.get().periods().get(0).startTime()).isEqualTo(Instant.parse("2026-08-08T19:00:00Z"));
        assertThat(got.get().periods().get(0).temperature().value()).isEqualTo(85);
        assertThat(got.get().generatedAt()).isNotNull();
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("hourly:nonexistent")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("hourly:expiring", hourly, Duration.ofMillis(500));
        assertThat(cache.get("hourly:expiring")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("hourly:expiring")).isEmpty();
    }

    @Test
    void putOverwritesExistingValue() {
        cache.put(KEY, hourly, Duration.ofMinutes(1));
        HourlyForecast other = new HourlyForecast(
                List.of(new HourlyForecastPeriod(
                        Instant.parse("2026-08-08T20:00:00Z"),
                        Temperature.fahrenheit(80),
                        "0 mph", "N", "Clear", false)),
                Instant.now(),
                "National Weather Service (api.weather.gov)"
        );
        cache.put(KEY, other, Duration.ofMinutes(1));
        assertThat(cache.get(KEY)).isPresent()
                .get()
                .satisfies(got -> {
                    assertThat(got.periods().get(0).startTime())
                            .isEqualTo(Instant.parse("2026-08-08T20:00:00Z"));
                    assertThat(got.periods().get(0).temperature().value()).isEqualTo(80);
                });
    }
}