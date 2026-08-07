package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;

/**
 * A single forecast period (e.g., "Tonight", "Monday").
 * Immutable.
 */
public record ForecastPeriod(
        String name,
        Temperature temperature,
        String windSpeed,
        String windDirection,
        String shortForecast,
        String detailedForecast,
        boolean daytime
) {

    public ForecastPeriod {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(temperature, "temperature must not be null");
        Objects.requireNonNull(shortForecast, "shortForecast must not be null");
        Objects.requireNonNull(detailedForecast, "detailedForecast must not be null");
        // windSpeed / windDirection may be null (calm conditions)
    }
}