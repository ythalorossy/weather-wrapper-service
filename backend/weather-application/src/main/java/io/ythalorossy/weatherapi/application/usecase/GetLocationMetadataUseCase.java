package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Use case that returns the NWS Weather Forecast Office (WFO) plus, if
 * available, today's sunrise/sunset for a city. Both pieces are best-effort:
 * if the WFO provider returns empty, the whole call is treated as a 404.
 * If the sun provider returns empty, the office is still returned with
 * {@code sunTimes = Optional.empty()}.
 */
public class GetLocationMetadataUseCase {

    private final LocationMetadataProvider metadataProvider;
    private final GetSunTimesUseCase getSunTimes;
    private final LocationResolver locationResolver;

    public GetLocationMetadataUseCase(
            LocationMetadataProvider metadataProvider,
            GetSunTimesUseCase getSunTimes,
            LocationResolver locationResolver) {
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        this.getSunTimes = Objects.requireNonNull(getSunTimes, "getSunTimes");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
    }

    /**
     * @throws LocationNotFoundException if the WFO provider cannot resolve
     *         the city (Nominatim miss propagated up via LocationResolver)
     * @return office + optional sun times
     */
    public Result execute(String cityName) {
        io.ythalorossy.weatherapi.domain.model.Location location = locationResolver.resolve(cityName);
        WeatherOffice office = metadataProvider
                .getOfficeFor(location)
                .orElseThrow(() -> new LocationNotFoundException(cityName));
        Optional<SunTimes> sun = getSunTimes.execute(cityName);
        return new Result(office, sun);
    }

    public record Result(WeatherOffice office, Optional<SunTimes> sunTimes) {}
}
