package io.ythalorossy.weatherapi.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.model.Temperature;
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
    static RedisJsonCache<List<Station>> stationsCache;
    static RedisJsonCache<List<StationObservation>> stationObservationsCache;

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
                Location.class, Location.class, new SimpleMeterRegistry());
        alertsCache = new RedisJsonCache<List<WeatherAlert>>(
                template, mapper, "alerts",
                (Class<List<WeatherAlert>>) (Class) List.class,
                WeatherAlert.class, new SimpleMeterRegistry());
        stationsCache = new RedisJsonCache<List<Station>>(
                template, mapper, "stations",
                (Class<List<Station>>) (Class) List.class,
                Station.class, new SimpleMeterRegistry());
        stationObservationsCache = new RedisJsonCache<List<StationObservation>>(
                template, mapper, "station-obs",
                (Class<List<StationObservation>>) (Class) List.class,
                StationObservation.class, new SimpleMeterRegistry());
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

    @Test
    void roundTripsListOfStationsAsTypedElements() {
        Station station = new Station("KDCA", "Washington Reagan", 38.85, -77.04);
        List<Station> value = List.of(station);
        stationsCache.put("arlington-va", value, Duration.ofMinutes(1));

        Optional<List<Station>> hit = stationsCache.get("arlington-va");
        assertThat(hit).isPresent();
        List<Station> stations = hit.get();
        assertThat(stations).hasSize(1);
        Station roundTripped = stations.get(0);
        assertThat(roundTripped).isInstanceOf(Station.class);
        assertThat(roundTripped.stationId()).isEqualTo("KDCA");
        assertThat(roundTripped.name()).isEqualTo("Washington Reagan");
        assertThat(roundTripped.latitude()).isEqualTo(38.85);
        assertThat(roundTripped.longitude()).isEqualTo(-77.04);
    }

    @Test
    void roundTripsListOfStationObservationsAsTypedElements() {
        StationObservation obs = new StationObservation(
                "KDCA",
                Instant.parse("2026-08-12T20:00:00Z"),
                Temperature.fahrenheit(82),
                65,
                "5 mph",
                "SW",
                "KDCA 122053Z 22004KT ...",
                29.95
        );
        List<StationObservation> value = List.of(obs);
        stationObservationsCache.put("KDCA", value, Duration.ofMinutes(1));

        Optional<List<StationObservation>> hit = stationObservationsCache.get("KDCA");
        assertThat(hit).isPresent();
        List<StationObservation> observations = hit.get();
        assertThat(observations).hasSize(1);
        StationObservation roundTripped = observations.get(0);
        assertThat(roundTripped).isInstanceOf(StationObservation.class);
        assertThat(roundTripped.stationId()).isEqualTo("KDCA");
        assertThat(roundTripped.temperature().value()).isEqualTo(82);
        assertThat(roundTripped.humidity()).isEqualTo(65);
        assertThat(roundTripped.barometricPressure()).isEqualTo(29.95);
    }
}