package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Top-level REST response for {@code GET /api/v1/weather?city=...}.
 *
 * <p>Mirrors the domain model with one ergonomic addition: a pre-formatted
 * temperature string ({@code "85°F"}) so clients don't need to assemble it.
 *
 * <p>{@link Schema} annotations surface in {@code /v3/api-docs} and the
 * Swagger UI. Examples are illustrative only — actual values depend on the
 * NWS response for the requested location.
 */
@Schema(description = "Forecast for a city: the resolved location and the multi-period forecast from the NWS.")
public record WeatherResponse(
        @Schema(description = "City as submitted by the caller (echoed back).",
                example = "Arlington, VA")
        String city,

        @Schema(description = "Coordinates resolved via Nominatim + a human-readable name.")
        LocationView resolvedLocation,

        @Schema(description = "Forecast payload from the National Weather Service.")
        ForecastView forecast
) {

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

            @Schema(description = "Forecast periods in chronological order (typically 7 days, day+night).")
            List<PeriodView> periods
    ) {
    }

    @Schema(description = "One forecast period (typically a 12-hour day or night block).")
    public record PeriodView(
            @Schema(description = "Period label, e.g. `Today`, `Tonight`, `Sunday`.",
                    example = "Today")
            String name,

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

            @Schema(description = "Long-form forecast with details on precipitation, hazards, etc.")
            String detailedForecast,

            @Schema(description = "True if this period is a daytime block; false for overnight.")
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
}