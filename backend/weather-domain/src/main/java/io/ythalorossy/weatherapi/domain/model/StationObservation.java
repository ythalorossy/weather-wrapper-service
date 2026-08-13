package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;

public record StationObservation(
        String stationId,
        Instant timestamp,
        Temperature temperature,
        Integer humidity,
        String windSpeed,
        String windDirection,
        String rawMessage,
        Double barometricPressure
) {
}
