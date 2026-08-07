package io.ythalorossy.weatherapi.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Top-level REST response for {@code GET /api/v1/weather?city=...}.
 *
 * <p>Mirrors the domain model with one ergonomic addition: a pre-formatted
 * temperature string ({@code "85°F"}) so clients don't need to assemble it.
 */
public record WeatherResponse(
        String city,
        LocationView resolvedLocation,
        ForecastView forecast
) {

    public record LocationView(
            double latitude,
            double longitude,
            String displayName
    ) {
    }

    public record ForecastView(
            Instant generatedAt,
            String source,
            List<PeriodView> periods
    ) {
    }

    public record PeriodView(
            String name,
            TemperatureView temperature,
            String windSpeed,
            String windDirection,
            String shortForecast,
            String detailedForecast,
            boolean daytime
    ) {
    }

    public record TemperatureView(
            int value,
            String unit,
            String formatted
    ) {
    }
}