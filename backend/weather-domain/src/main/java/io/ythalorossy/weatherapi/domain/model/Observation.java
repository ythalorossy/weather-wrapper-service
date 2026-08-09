package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single weather observation from an NWS observation station. Captures the
 * latest reported conditions (typically 5\u201315 min cadence).
 *
 * <p>Fields are converted to consumer-friendly units (Fahrenheit, mph, percent)
 * at the adapter layer; the domain stays free of unit-conversion logic.
 */
public record Observation(
        String stationId,
        String stationName,
        Instant timestamp,
        Double temperatureFahrenheit,
        Double dewpointFahrenheit,
        Double windSpeedMph,
        Integer windDirectionDegrees,
        String windDirectionCompass,
        Double relativeHumidityPercent,
        Double barometricPressureInHg,
        String textDescription
) {

    public Observation {
        Objects.requireNonNull(stationId, "stationId must not be null");
        Objects.requireNonNull(stationName, "stationName must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        // Temperature/dewpoint/wind/humidity/pressure may be null (sensor offline)
        // textDescription may be null (sensor offline)
        // windDirection may be null (calm)
    }
}