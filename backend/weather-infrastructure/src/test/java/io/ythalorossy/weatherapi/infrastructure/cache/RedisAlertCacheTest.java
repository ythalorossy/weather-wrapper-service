package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
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
class RedisAlertCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisAlertCache cache;

    private static final String KEY = "alerts:38.88,-77.09";
    private final List<WeatherAlert> alerts = List.of(
            new WeatherAlert(
                    "urn:oid:2.49.0.1.test1",
                    "Severe Thunderstorm Warning",
                    AlertSeverity.Severe,
                    AlertCertainty.Likely,
                    AlertUrgency.Expected,
                    AlertCategory.Met,
                    "Severe Thunderstorm Warning issued ...",
                    "Long description...",
                    "Take shelter.",
                    "Arlington County",
                    Instant.parse("2026-08-08T21:53:00-04:00"),
                    Instant.parse("2026-08-08T21:53:00-04:00"),
                    Instant.parse("2026-08-08T22:30:00-04:00"),
                    "https://example.com"
            )
    );

    @Test
    void putThenGetRoundtripsList() {
        cache.put(KEY, alerts, Duration.ofMinutes(1));

        Optional<List<WeatherAlert>> got = cache.get(KEY);

        assertThat(got).isPresent();
        assertThat(got.get()).hasSize(1);
        assertThat(got.get().get(0).event()).isEqualTo("Severe Thunderstorm Warning");
    }

    @Test
    void emptyListRoundtrips() {
        cache.put(KEY, List.of(), Duration.ofMinutes(1));
        Optional<List<WeatherAlert>> got = cache.get(KEY);

        assertThat(got).isPresent();
        assertThat(got.get()).isEmpty();
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("alerts:nonexistent")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("alerts:expiring", alerts, Duration.ofMillis(500));
        assertThat(cache.get("alerts:expiring")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("alerts:expiring")).isEmpty();
    }
}