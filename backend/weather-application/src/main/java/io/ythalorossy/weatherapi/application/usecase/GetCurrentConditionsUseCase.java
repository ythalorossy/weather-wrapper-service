package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Use case: current conditions at the nearest NWS observation station to a city.
 *
 * <p>Cache key is the resolved lat/lon (e.g., {@code obs:38.88,-77.09}) so
 * the same city always hits the same cache slot. 10-minute TTL.
 */
public class GetCurrentConditionsUseCase {

    private static final String OBS_KEY_PREFIX = "obs:";

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

    /**
     * @return the latest observation
     * @throws IllegalStateException if no nearby station is available
     */
    public Observation execute(String cityName) {
        Location location = locationResolver.resolve(cityName);

        String obsKey = location.observationCacheKey().substring(OBS_KEY_PREFIX.length());
        return CacheAside.getOrLoad(
                observationCache, obsKey, observationCacheTtl,
                () -> observationProvider.getCurrentObservation(location)
                        .orElseThrow(() -> new IllegalStateException(
                                "Observation provider returned empty for " + location.displayName())));
    }
}
