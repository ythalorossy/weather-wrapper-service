package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;

/**
 * Geographic location value object. Immutable.
 *
 * <p>Represents a point on Earth resolved from a city name via a
 * {@link io.ythalorossy.weatherapi.domain.port.GeocodingProvider}.
 */
public record Location(
        double latitude,
        double longitude,
        String displayName
) {

    public Location {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude out of range [-90,90]: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude out of range [-180,180]: " + longitude);
        }
        Objects.requireNonNull(displayName, "displayName must not be null");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }

    /**
     * Stable cache key for weather data derived from this location.
     * Lat/lon rounded to 2 decimal places (≈ 1.1 km precision) so trivial
     * floating-point or naming variations don't fragment cache slots.
     */
    public String weatherCacheKey() {
        return String.format("weather:%.2f,%.2f", latitude, longitude);
    }
}