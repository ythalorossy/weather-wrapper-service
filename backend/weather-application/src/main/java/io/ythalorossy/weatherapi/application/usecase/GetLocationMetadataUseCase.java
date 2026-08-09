package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Use case that returns the NWS Weather Forecast Office (WFO) for a city:
 * timezone, radar station, human-readable name, disclaimer URL.
 *
 * <p>No cache: office data is per-city, called once per request from the
 * client UI. The upstream response itself is already cached by NWS.
 */
public class GetLocationMetadataUseCase {

    private final LocationMetadataProvider metadataProvider;
    private final LocationResolver locationResolver;

    public GetLocationMetadataUseCase(
            LocationMetadataProvider metadataProvider,
            LocationResolver locationResolver) {
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
    }

    /**
     * @return the {@link WeatherOffice} for {@code cityName}, or empty if
     *         upstream could not resolve the office.
     */
    public Optional<WeatherOffice> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return metadataProvider.getOfficeFor(location);
    }
}