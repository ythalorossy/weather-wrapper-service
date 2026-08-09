package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.port.ObservationCache;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case: current conditions at the nearest NWS observation station to a city.
 *
 * <p>Cache key is the resolved lat/lon (e.g., {@code obs:38.88,-77.09}) so
 * the same city always hits the same cache slot. 10-minute TTL.
 */
public class GetCurrentConditionsUseCase {

    private final ObservationProvider observationProvider;
    private final ObservationCache observationCache;
    private final LocationResolver locationResolver;
    private final Duration observationCacheTtl;

    public GetCurrentConditionsUseCase(
            ObservationProvider observationProvider,
            ObservationCache observationCache,
            LocationResolver locationResolver,
            Duration observationCacheTtl) {
        this.observationProvider = Objects.requireNonNull(observationProvider, "observationProvider");
        this.observationCache = Objects.requireNonNull(observationCache, "observationCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.observationCacheTtl = requirePositive(observationCacheTtl, "observationCacheTtl");
    }

    /**
     * @return the latest observation, or empty if no nearby station or upstream
     *         could not be reached.
     */
    public Optional<Observation> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);

        String key = location.observationCacheKey();
        Optional<Observation> cached = observationCache.get(key);
        if (cached.isPresent()) return cached;

        Optional<Observation> fresh = observationProvider.getCurrentObservation(location);
        if (fresh.isEmpty()) return Optional.empty();

        observationCache.put(key, fresh.get(), observationCacheTtl);
        return fresh;
    }

    private static Duration requirePositive(Duration ttl, String name) {
        Objects.requireNonNull(ttl, name);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + ttl);
        }
        return ttl;
    }
}