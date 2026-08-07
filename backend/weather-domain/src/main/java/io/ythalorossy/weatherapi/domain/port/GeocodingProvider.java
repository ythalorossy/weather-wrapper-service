package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;

import java.util.Optional;

/**
 * Port (driven adapter contract): resolves a free-text city name to a geographic location.
 *
 * <p>Implementations live in the infrastructure layer. The domain depends only on this
 * interface, so the upstream geocoder (Nominatim, Photon, Google, static lookup, ...) can
 * be swapped without touching the domain or application code.
 */
public interface GeocodingProvider {

    /**
     * Resolves the given city name (e.g., {@code "Arlington, VA"}) to a {@link Location}.
     *
     * @param cityName user-entered city name; must not be null or blank
     * @return the resolved location, or {@link Optional#empty()} if no match was found
     * @throws IllegalArgumentException if {@code cityName} is null or blank
     */
    Optional<Location> findLocation(String cityName);
}