package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;

import java.util.Optional;

/**
 * Outbound port for the per-location metadata not already in the basic
 * forecast: the issuing NWS Weather Forecast Office, its radar station,
 * and so on. Sourced from {@code /points} + {@code /offices/{id}}.
 */
public interface LocationMetadataProvider {

    /**
     * Returns the WFO metadata for {@code location}, or empty if upstream
     * could not resolve the office.
     */
    Optional<WeatherOffice> getOfficeFor(Location location);
}