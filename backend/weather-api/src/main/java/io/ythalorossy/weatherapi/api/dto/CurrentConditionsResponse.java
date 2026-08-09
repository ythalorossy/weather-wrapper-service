package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.ythalorossy.weatherapi.domain.model.Observation;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * REST response for {@code GET /api/v1/conditions?city=...}. Carries the
 * latest observation from the nearest NWS station, with consumer-friendly
 * units (Fahrenheit, mph, inHg, percent) already applied at the adapter.
 */
@Schema(description = "Current weather conditions from the nearest NWS observation station.")
public record CurrentConditionsResponse(
        @Schema(description = "City as submitted by the caller (echoed back).",
                example = "Arlington, VA")
        String city,

        @Schema(description = "Coordinates resolved via Nominatim + a human-readable name.")
        WeatherResponse.LocationView resolvedLocation,

        @Schema(description = "Latest observation from the nearest station.")
        ObservationView observation
) {

    @Schema(description = "Latest observation with human-readable units.")
    public record ObservationView(
            @Schema(description = "NWS station id (e.g., KIAD for Dulles).",
                    example = "KIAD")
            String stationId,

            @Schema(description = "Human-readable station name.",
                    example = "Washington/Dulles International Airport, VA")
            String stationName,

            @Schema(description = "ISO-8601 timestamp the observation was reported.",
                    example = "2026-08-08T21:35:00Z")
            String timestamp,

            @Schema(description = "Formatted local-time string for the user (browser tz).",
                    example = "5:35 PM")
            String timestampLocal,

            @Schema(description = "Air temperature in degrees Fahrenheit.",
                    example = "78.4")
            Double temperatureFahrenheit,

            @Schema(description = "Dewpoint temperature in degrees Fahrenheit.",
                    example = "71.6")
            Double dewpointFahrenheit,

            @Schema(description = "Wind speed in mph.",
                    example = "5.2")
            Double windSpeedMph,

            @Schema(description = "Wind direction in degrees from true north (0-359).",
                    example = "315")
            Integer windDirectionDegrees,

            @Schema(description = "Wind direction as a 16-point compass label.",
                    example = "NW")
            String windDirectionCompass,

            @Schema(description = "Relative humidity (0-100).",
                    example = "83.5")
            Double relativeHumidityPercent,

            @Schema(description = "Barometric pressure in inches of mercury.",
                    example = "30.02")
            Double barometricPressureInHg,

            @Schema(description = "Free-text conditions summary.",
                    example = "Cloudy")
            String textDescription
    ) {
        public static ObservationView from(Observation o) {
            String local = o.timestamp() == null
                    ? null
                    : DateTimeFormatter.ofPattern("h:mm a")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(o.timestamp());
            return new ObservationView(
                    o.stationId(),
                    o.stationName(),
                    o.timestamp() == null ? null : o.timestamp().toString(),
                    local,
                    round1(o.temperatureFahrenheit()),
                    round1(o.dewpointFahrenheit()),
                    round1(o.windSpeedMph()),
                    o.windDirectionDegrees(),
                    o.windDirectionCompass(),
                    round1(o.relativeHumidityPercent()),
                    round1(o.barometricPressureInHg()),
                    o.textDescription()
            );
        }

        private static Double round1(Double v) {
            return v == null ? null : Math.round(v * 10.0) / 10.0;
        }
    }
}