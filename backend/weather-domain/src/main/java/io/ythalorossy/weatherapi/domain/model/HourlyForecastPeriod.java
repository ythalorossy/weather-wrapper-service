package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single hourly forecast period. Captures one hour's worth of forecast data.
 *
 * <p>Distinct from {@link ForecastPeriod} (12-hour blocks) by its finer time
 * resolution and an explicit {@link Instant} timestamp rather than a label.
 */
public record HourlyForecastPeriod(
        Instant startTime,
        Temperature temperature,
        String windSpeed,
        String windDirection,
        String shortForecast,
        boolean daytime
) {

    public HourlyForecastPeriod {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(temperature, "temperature must not be null");
        Objects.requireNonNull(shortForecast, "shortForecast must not be null");
        // windSpeed / windDirection may be null (calm conditions)
    }
}