package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;

import java.time.Duration;
import java.util.Objects;

public class GetCurrentConditionsUseCase {

    private static final String OBS_NS = "obs";

    private final ObservationProvider observationProvider;
    private final Cache<Observation> observationCache;
    private final LocationResolver locationResolver;
    private final Duration observationCacheTtl;

    public GetCurrentConditionsUseCase(
            ObservationProvider observationProvider,
            Cache<Observation> observationCache,
            LocationResolver locationResolver,
            Duration observationCacheTtl) {
        this.observationProvider = Objects.requireNonNull(observationProvider, "observationProvider");
        this.observationCache = Objects.requireNonNull(observationCache, "observationCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        CacheAside.requirePositive(observationCacheTtl, "observationCacheTtl");
        this.observationCacheTtl = observationCacheTtl;
    }

    public Observation execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return CacheAside.getOrLoad(
                observationCache, location.cacheKey(OBS_NS).substring(OBS_NS.length() + 1), observationCacheTtl,
                () -> observationProvider.getCurrentObservation(location)
                        .orElseThrow(() -> new IllegalStateException(
                                "Observation provider returned empty for " + location.displayName())));
    }
}
