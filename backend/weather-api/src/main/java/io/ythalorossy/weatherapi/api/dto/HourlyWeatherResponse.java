package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.HourlyForecastPeriod;

import java.time.Instant;
import java.util.List;

/**
 * REST response for {@code GET /api/v1/weather/hourly?city=...}.
 *
 * <p>Mirrors {@link WeatherResponse}'s shape but with finer-grained hourly
 * periods (no {@code name} field, but explicit {@code startTime}).
 */
@Schema(description = "Hourly forecast for a city: the resolved location and a list of hourly forecast periods from the NWS.")
public record HourlyWeatherResponse(
        @Schema(description = "City as submitted by the caller (echoed back).",
                example = "Arlington, VA")
        String city,

        @Schema(description = "Coordinates resolved via Nominatim + a human-readable name.")
        WeatherResponse.LocationView resolvedLocation,

        @Schema(description = "Hourly forecast payload from the National Weather Service.")
        ForecastView forecast
) {

    @Schema(description = "Forecast envelope from the National Weather Service.")
    public record ForecastView(
            @Schema(description = "ISO-8601 timestamp at which the NWS generated this forecast.",
                    example = "2026-08-07T12:00:00Z")
            Instant generatedAt,

            @Schema(description = "Upstream provider name.",
                    example = "National Weather Service (api.weather.gov)")
            String source,

            @Schema(description = "Hourly forecast periods in chronological order (up to ~156 hours).")
            List<PeriodView> periods
    ) {
    }

    @Schema(description = "One hourly forecast period (one hour).")
    public record PeriodView(
            @Schema(description = "ISO-8601 timestamp at which this period starts.",
                    example = "2026-08-08T19:00:00Z")
            Instant startTime,

            @Schema(description = "Temperature for this period.")
            WeatherResponse.TemperatureView temperature,

            @Schema(description = "Wind speed as a human string, e.g. `5 mph`.",
                    example = "5 mph")
            String windSpeed,

            @Schema(description = "Wind direction abbreviation.",
                    example = "NW")
            String windDirection,

            @Schema(description = "Short headline forecast.",
                    example = "Sunny")
            String shortForecast,

            @Schema(description = "True if this period is a daytime hour; false otherwise.")
            boolean daytime
    ) {
    }

    private static PeriodView toPeriodView(HourlyForecastPeriod p) {
        return new PeriodView(
                p.startTime(),
                new WeatherResponse.TemperatureView(p.temperature().value(), p.temperature().unit().name(), p.temperature().formatted()),
                p.windSpeed(),
                p.windDirection(),
                p.shortForecast(),
                p.daytime()
        );
    }
}