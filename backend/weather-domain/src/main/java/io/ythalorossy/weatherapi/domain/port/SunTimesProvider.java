package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider of sunrise/sunset times for a location on a specific date.
 *
 * <p>Implementations are expected to be deterministic for given inputs
 * and side-effect free (no I/O). The application layer is responsible for
 * any caching.
 */
public interface SunTimesProvider {

    /**
     * @param location resolved location; must not be null
     * @param date     the date for which to compute sun times; must not be null
     * @param zone     timezone of the location; must not be null
     * @return sunrise/sunset times for the date in the location's timezone,
     *         or empty if the sun does not rise or set on that date at that
     *         latitude (polar regions near solstice)
     */
    Optional<SunTimes> getSunTimes(Location location, LocalDate date, ZoneId zone);

    default SunTimes requireSunTimes(Location location, LocalDate date, ZoneId zone) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");
        return getSunTimes(location, date, zone).orElseThrow(() ->
                new IllegalStateException("No sun times available for "
                        + location.displayName() + " on " + date));
    }
}