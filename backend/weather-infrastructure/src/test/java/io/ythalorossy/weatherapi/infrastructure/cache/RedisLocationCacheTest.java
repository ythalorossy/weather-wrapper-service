package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.Location;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
@Testcontainers
class RedisLocationCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisLocationCache cache;

    private static final String KEY = "geo:arlington, va";
    private final Location arlington = new Location(38.8816, -77.0910,
            "Arlington, Arlington County, Virginia, United States");

    @Test
    void putThenGetRoundtripsValue() {
        cache.put(KEY, arlington, Duration.ofMinutes(1));

        Optional<Location> got = cache.get(KEY);

        assertThat(got).isPresent();
        assertThat(got.get().latitude()).isEqualTo(38.8816);
        assertThat(got.get().longitude()).isEqualTo(-77.0910);
        assertThat(got.get().displayName())
                .isEqualTo("Arlington, Arlington County, Virginia, United States");
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("geo:nonexistent")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("geo:expiring", arlington, Duration.ofMillis(500));
        assertThat(cache.get("geo:expiring")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("geo:expiring")).isEmpty();
    }

    @Test
    void putOverwritesExistingValue() {
        Location one = new Location(38.8816, -77.0910, "First");
        Location two = new Location(40.7128, -74.0060, "Second");
        cache.put(KEY, one, Duration.ofMinutes(1));
        cache.put(KEY, two, Duration.ofMinutes(1));
        assertThat(cache.get(KEY)).isPresent()
                .get()
                .extracting(Location::displayName)
                .isEqualTo("Second");
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> cache.put(null, arlington, Duration.ofMinutes(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.put(KEY, null, Duration.ofMinutes(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.put(KEY, arlington, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.get(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> cache.put(KEY, arlington, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(KEY, arlington, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}