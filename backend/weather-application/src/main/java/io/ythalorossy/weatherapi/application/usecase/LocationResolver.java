package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Shared city-name → {@link Location} resolver with cache-aside + negative
 * caching. Extracted from {@link GetWeatherUseCase} so every use case that
 * takes a city name resolves the location identically.
 *
 * <p>Flow per call:
 * <ol>
 *   <li>Positive cache hit → return.</li>
 *   <li>Negative cache hit → throw {@link LocationNotFoundException}
 *       without an upstream call.</li>
 *   <li>Cache miss on both → call geocoder. On success, write through
 *       (positive entry, long TTL). On miss, mark as absent (negative
 *       entry, short TTL) and throw.</li>
 * </ol>
 *
 * <p>Plain Java (no Spring annotations), instantiated by {@code @Configuration}
 * beans in the API module.
 */
public class LocationResolver {

    private final GeocodingProvider geocodingProvider;
    private final Cache<Location> locationCache;
    private final Duration locationCacheTtl;
    private final Duration locationAbsentTtl;

    public LocationResolver(
            GeocodingProvider geocodingProvider,
            Cache<Location> locationCache,
            Duration locationCacheTtl,
            Duration locationAbsentTtl) {
        this.geocodingProvider = Objects.requireNonNull(geocodingProvider, "geocodingProvider");
        this.locationCache = Objects.requireNonNull(locationCache, "locationCache");
        this.locationCacheTtl = locationCacheTtl;
        this.locationAbsentTtl = locationAbsentTtl;
    }

    /**
     * @throws LocationNotFoundException if the city cannot be resolved to a location
     * @throws IllegalArgumentException  if {@code cityName} is null or blank
     */
    public Location resolve(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName must not be blank");
        }

        String key = Location.geocodingCacheKey(cityName);

        var cached = locationCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (locationCache.isAbsent(key)) {
            throw new LocationNotFoundException(cityName);
        }

        Location fresh = geocodingProvider.findLocation(cityName)
                .orElseThrow(() -> {
                    locationCache.markAbsent(key, locationAbsentTtl);
                    return new LocationNotFoundException(cityName);
                });

        locationCache.put(key, fresh, locationCacheTtl);
        return fresh;
    }
}
