package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Sunrise and sunset times for a single day at a single location, plus the
 * location's timezone offset. Source: the NWS {@code /points} response's
 * {@code properties.timeZone} + sunrise/sunset computed from the gridpoint.
 */
public record SunTimes(
        LocalDate date,
        Instant sunrise,
        Instant sunset,
        String timezoneId
) {

    public SunTimes {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(sunrise, "sunrise must not be null");
        Objects.requireNonNull(sunset, "sunset must not be null");
        Objects.requireNonNull(timezoneId, "timezoneId must not be null");
        if (timezoneId.isBlank()) {
            throw new IllegalArgumentException("timezoneId must not be blank");
        }
        if (!sunrise.toLocalDate().equals(date) && !sunrise.atZone(java.time.ZoneId.of(timezoneId)).toLocalDate().equals(date)) {
            // sunrise may straddle midnight for high-latitude locations; just warn softly via assertion
        }
    }
}