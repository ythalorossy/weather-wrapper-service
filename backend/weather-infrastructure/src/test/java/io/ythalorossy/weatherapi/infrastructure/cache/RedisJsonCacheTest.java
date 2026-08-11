package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.Location;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class RedisJsonCacheTest {

    static GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
    static RedisJsonCache<Location> cache;

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        REDIS.start();
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }

    @BeforeAll
    static void setup(@Autowired StringRedisTemplate template) {
        cache = new RedisJsonCache<>(
                template, new ObjectMapper(), "geo",
                Location.class, new SimpleMeterRegistry());
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
}
