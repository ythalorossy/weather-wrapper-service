package io.ythalorossy.weatherapi.infrastructure.cache;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
@Testcontainers
class RedisWeatherCacheTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    RedisWeatherCache cache;

    private static final String KEY = "weather:test:38.88,-77.09";
    private final WeatherForecast forecast = WeatherForecast.of(
            List.of(new ForecastPeriod("Today", Temperature.fahrenheit(85),
                    "5 mph", "NW", "Sunny", "Sunny, with a high near 85.", true)),
            "National Weather Service (api.weather.gov)"
    );

    @Test
    void putThenGetRoundtripsValue() {
        cache.put(KEY, forecast, Duration.ofMinutes(1));

        Optional<WeatherForecast> got = cache.get(KEY);

        assertThat(got).isPresent();
        assertThat(got.get().source()).isEqualTo("National Weather Service (api.weather.gov)");
        assertThat(got.get().periods()).hasSize(1);
        assertThat(got.get().periods().get(0).name()).isEqualTo("Today");
        assertThat(got.get().periods().get(0).temperature().value()).isEqualTo(85);
        assertThat(got.get().generatedAt()).isNotNull();
    }

    @Test
    void getMissingKeyReturnsEmpty() {
        assertThat(cache.get("weather:nonexistent")).isEmpty();
    }

    @Test
    void ttlExpiresValue() throws InterruptedException {
        cache.put("weather:expiring", forecast, Duration.ofMillis(500));
        assertThat(cache.get("weather:expiring")).isPresent();
        Thread.sleep(900);
        assertThat(cache.get("weather:expiring")).isEmpty();
    }

    @Test
    void putOverwritesExistingValue() {
        cache.put(KEY, forecast, Duration.ofMinutes(1));
        WeatherForecast other = WeatherForecast.of(
                List.of(new ForecastPeriod("Tomorrow", Temperature.fahrenheit(88),
                        "3 mph", "W", "Clear", "Clear.", true)),
                "NWS"
        );
        cache.put(KEY, other, Duration.ofMinutes(1));
        assertThat(cache.get(KEY)).isPresent()
                .get()
                .extracting(WeatherForecast::periods)
                .satisfies(p -> assertThat(p).hasSize(1));
        assertThat(cache.get(KEY).get().periods().get(0).name()).isEqualTo("Tomorrow");
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> cache.put(null, forecast, Duration.ofMinutes(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.put(KEY, null, Duration.ofMinutes(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.put(KEY, forecast, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.get(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> cache.put(KEY, forecast, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(KEY, forecast, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}