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
        LocationView resolvedLocation,

        @Schema(description = "Hourly forecast payload from the National Weather Service.")
        ForecastView forecast
) {

    public static HourlyWeatherResponse from(String requestedCity, HourlyForecast forecast) {
        List<PeriodView> periods = forecast.periods().stream()
                .map(HourlyWeatherResponse::toPeriodView)
                .toList();
        return new HourlyWeatherResponse(
                requestedCity,
                null, // filled by controller to avoid coupling domain to API layer
                new ForecastView(forecast.generatedAt(), forecast.source(), periods)
        );
    }

    @Schema(description = "Lat/lon + display name for the resolved city.")
    public record LocationView(
            @Schema(description = "Latitude in decimal degrees.", example = "38.8816")
            double latitude,

            @Schema(description = "Longitude in decimal degrees.", example = "-77.0910")
            double longitude,

            @Schema(description = "Human-readable name (city, county, state, country).",
                    example = "Arlington, Arlington County, Virginia, United States")
            String displayName
    ) {
    }

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
            TemperatureView temperature,

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

    @Schema(description = "Temperature value + unit + pre-formatted display string.")
    public record TemperatureView(
            @Schema(description = "Numeric temperature value (in `unit`).", example = "85")
            int value,

            @Schema(description = "Unit code (`FAHRENHEIT` or `CELSIUS`).", example = "FAHRENHEIT")
            String unit,

            @Schema(description = "Pre-formatted string with degree symbol for direct display.",
                    example = "85\u00b0F")
            String formatted
    ) {
    }

    private static PeriodView toPeriodView(HourlyForecastPeriod p) {
        return new PeriodView(
                p.startTime(),
                new TemperatureView(p.temperature().value(), p.temperature().unit().name(), p.temperature().formatted()),
                p.windSpeed(),
                p.windDirection(),
                p.shortForecast(),
                p.daytime()
        );
    }
}