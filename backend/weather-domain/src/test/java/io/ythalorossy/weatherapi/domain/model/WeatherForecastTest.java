package io.ythalorossy.weatherapi.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherForecastTest {

    private final List<ForecastPeriod> periods = List.of(
            new ForecastPeriod("Today", Temperature.fahrenheit(85), "5 mph", "NW",
                    "Sunny", "Sunny, with a high near 85.", true)
    );

    @Test
    void constructsValidForecast() {
        WeatherForecast fc = new WeatherForecast(periods, Instant.now(), "NWS");
        assertThat(fc.periods()).hasSize(1);
        assertThat(fc.source()).isEqualTo("NWS");
    }

    @Test
    void rejectsEmptyPeriods() {
        assertThatThrownBy(() -> new WeatherForecast(List.of(), Instant.now(), "NWS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periods");
    }

    @Test
    void rejectsNullPeriods() {
        assertThatThrownBy(() -> new WeatherForecast(null, Instant.now(), "NWS"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullGeneratedAt() {
        assertThatThrownBy(() -> new WeatherForecast(periods, null, "NWS"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSource() {
        assertThatThrownBy(() -> new WeatherForecast(periods, Instant.now(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankSource() {
        assertThatThrownBy(() -> new WeatherForecast(periods, Instant.now(), "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void periodsListIsImmutable() {
        WeatherForecast fc = new WeatherForecast(periods, Instant.now(), "NWS");
        assertThatThrownBy(() -> fc.periods().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void factorySetsGeneratedAtToNow() {
        Instant before = Instant.now();
        WeatherForecast fc = WeatherForecast.of(periods, "NWS");
        Instant after = Instant.now();
        assertThat(fc.generatedAt()).isBetween(before, after);
    }

    @Test
    void factoryDefensiveCopyOnInputList() {
        List<ForecastPeriod> mutable = new ArrayList<>(periods);
        WeatherForecast fc = WeatherForecast.of(mutable, "NWS");
        mutable.clear();
        assertThat(fc.periods()).hasSize(1);
    }
}