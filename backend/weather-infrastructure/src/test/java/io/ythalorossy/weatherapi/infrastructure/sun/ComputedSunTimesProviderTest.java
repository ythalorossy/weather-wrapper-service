package io.ythalorossy.weatherapi.infrastructure.sun;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ComputedSunTimesProviderTest {

    private ComputedSunTimesProvider provider;

    @BeforeEach
    void setUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        provider = new ComputedSunTimesProvider(registry);
    }

    @Test
    void computesArlingtonVAMidSummer() {
        Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
        LocalDate date = LocalDate.of(2026, 6, 21);

        Optional<SunTimes> result = provider.getSunTimes(arlington, date, ZoneId.of("America/New_York"));

        assertThat(result).isPresent();
        SunTimes times = result.get();
        assertThat(times.date()).isEqualTo(date);
        assertThat(times.timezoneId()).isEqualTo("America/New_York");
        int sunriseHour = times.sunrise().atZone(ZoneId.of(times.timezoneId())).getHour();
        int sunsetHour = times.sunset().atZone(ZoneId.of(times.timezoneId())).getHour();
        assertThat(sunriseHour).isBetween(5, 6);
        assertThat(sunsetHour).isBetween(20, 21);
    }

    @Test
    void computesHonoluluMidWinter() {
        Location honolulu = new Location(21.3069, -157.8583, "Honolulu, HI");
        LocalDate date = LocalDate.of(2026, 12, 21);

        Optional<SunTimes> result = provider.getSunTimes(honolulu, date, ZoneId.of("Pacific/Honolulu"));

        assertThat(result).isPresent();
        SunTimes times = result.get();
        assertThat(times.timezoneId()).isEqualTo("Pacific/Honolulu");
        int sunriseHour = times.sunrise().atZone(ZoneId.of(times.timezoneId())).getHour();
        int sunsetHour = times.sunset().atZone(ZoneId.of(times.timezoneId())).getHour();
        assertThat(sunriseHour).isBetween(6, 7);
        assertThat(sunsetHour).isBetween(17, 18);
    }

    @Test
    void returnsResultForReykjavikMidSummer() {
        // Note: solarpositioning 2.0.3 returns RegularDay instead of AllDay for
        // extreme latitudes. The library doesn't properly handle polar day/night.
        // This test verifies we gracefully return a result rather than crashing.
        Location reykjavik = new Location(64.1466, -21.9426, "Reykjavik, Iceland");
        LocalDate date = LocalDate.of(2026, 6, 21);

        Optional<SunTimes> result = provider.getSunTimes(reykjavik, date, ZoneId.of("Atlantic/Reykjavik"));

        assertThat(result).isPresent();
    }
}