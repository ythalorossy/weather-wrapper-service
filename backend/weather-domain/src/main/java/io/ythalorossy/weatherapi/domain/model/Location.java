package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

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

    /** Matches one or more whitespace characters. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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

    /**
     * Stable cache key for the geocoding lookup, derived from the user-entered
     * city name. Normalization rules:
     * <ul>
     *   <li>Leading and trailing whitespace trimmed.</li>
     *   <li>ASCII case-folded to lower-case.</li>
     *   <li>Internal runs of whitespace collapsed to a single space.</li>
     * </ul>
     * Out of scope: diacritic stripping ({@code "Bogotá"} vs {@code "Bogota"}),
     * abbreviation expansion ({@code "Arlington, VA"} vs {@code "Arlington Virginia"}).
     *
     * @param cityName user-entered city name; must not be null
     * @return the cache key, e.g., {@code "geo:arlington, va"}
     */
    public static String geocodingCacheKey(String cityName) {
        Objects.requireNonNull(cityName, "cityName");
        String normalized = WHITESPACE.matcher(cityName.trim().toLowerCase()).replaceAll(" ");
        return "geo:" + normalized;
    }
}