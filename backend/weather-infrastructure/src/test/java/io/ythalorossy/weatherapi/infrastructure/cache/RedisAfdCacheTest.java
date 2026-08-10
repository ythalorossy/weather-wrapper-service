package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;
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
class RedisAfdCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisAfdCache cache;

    private final AfdProduct sample = new AfdProduct(
            "KLWX",
            Instant.parse("2026-08-10T14:35:00Z"),
            "KLWX AFD\n\n.SHORT TERM...\n\nDry weather through Tuesday.");

    @Test
    void putThenGetRoundtripsValue() {
        cache.put("afd:KLWX", sample, Duration.ofMinutes(1));
        Optional<AfdProduct> got = cache.get("afd:KLWX");
        assertThat(got).isPresent();
        assertThat(got.get().officeId()).isEqualTo("KLWX");
        assertThat(got.get().issuanceTime()).isEqualTo(Instant.parse("2026-08-10T14:35:00Z"));
        assertThat(got.get().body()).contains("Dry weather through Tuesday.");
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("afd:nonexistent")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("afd:expiring", sample, Duration.ofMillis(500));
        assertThat(cache.get("afd:expiring")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("afd:expiring")).isEmpty();
    }

    @Test
    void differentOfficesDoNotCollide() {
        AfdProduct klwx = sample;
        AfdProduct kbox = new AfdProduct(
                "KBOX",
                Instant.parse("2026-08-10T15:00:00Z"),
                "KBOX AFD\n\nDry.");
        cache.put("afd:KLWX", klwx, Duration.ofMinutes(1));
        cache.put("afd:KBOX", kbox, Duration.ofMinutes(1));

        assertThat(cache.get("afd:KLWX")).contains(klwx);
        assertThat(cache.get("afd:KBOX")).contains(kbox);
    }
}
