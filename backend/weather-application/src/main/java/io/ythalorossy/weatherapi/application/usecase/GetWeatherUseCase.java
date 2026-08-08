package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.LocationCache;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Cache-aside orchestration for the weather query use case.
 *
 * <p>Two-layer cache with negative caching on the geocoding side:
 * <ol>
 *   <li>Geocoding cache: city name → {@link Location}, with negative entries
 *       (city not found) cached briefly. Both positive and negative entries
 *       live behind {@link LocationCache}; positive TTL is long (days),
 *       negative TTL is short (seconds-to-minutes).</li>
 *   <li>Weather cache: location → {@link WeatherForecast}. TTL configurable,
 *       default 12 h.</li>
 * </ol>
 *
 * <p>Flow per request:
 * <ol>
 *   <li>Check geocoding cache for a positive entry; if present, use it.</li>
 *   <li>Check geocoding cache for a negative entry; if present, throw
 *       {@link LocationNotFoundException} without hitting Nominatim.</li>
 *   <li>Call Nominatim. On hit, write through to cache. On miss, mark the
 *       key as absent (short TTL) and throw.</li>
 *   <li>Compute the weather cache key from the resolved Location.</li>
 *   <li>Check weather cache; on hit return immediately. On miss, fetch from
 *       NWS and write through.</li>
 * </ol>
 *
 * <p>This class is plain Java (no Spring annotations). It is instantiated by a
 * {@code @Configuration} bean in the API module so the application layer stays
 * framework-agnostic.
 */
public class GetWeatherUseCase {

    private final GeocodingProvider geocodingProvider;
    private final WeatherProvider weatherProvider;
    private final WeatherCache weatherCache;
    private final LocationCache locationCache;
    private final Duration weatherCacheTtl;
    private final Duration locationCacheTtl;
    private final Duration locationAbsentTtl;

    public GetWeatherUseCase(
            GeocodingProvider geocodingProvider,
            WeatherProvider weatherProvider,
            WeatherCache weatherCache,
            LocationCache locationCache,
            Duration weatherCacheTtl,
            Duration locationCacheTtl,
            Duration locationAbsentTtl) {
        this.geocodingProvider = Objects.requireNonNull(geocodingProvider, "geocodingProvider");
        this.weatherProvider = Objects.requireNonNull(weatherProvider, "weatherProvider");
        this.weatherCache = Objects.requireNonNull(weatherCache, "weatherCache");
        this.locationCache = Objects.requireNonNull(locationCache, "locationCache");
        this.weatherCacheTtl = requirePositive(weatherCacheTtl, "weatherCacheTtl");
        this.locationCacheTtl = requirePositive(locationCacheTtl, "locationCacheTtl");
        this.locationAbsentTtl = requirePositive(locationAbsentTtl, "locationAbsentTtl");
    }

    /**
     * Executes the weather query for the given city name.
     *
     * @param cityName user-entered city (e.g., "Arlington, VA"); must not be null or blank
     * @return the resolved location plus the forecast (cached or freshly fetched)
     * @throws LocationNotFoundException if the city cannot be resolved to a location
     * @throws IllegalArgumentException  if {@code cityName} is null or blank
     */
    public WeatherQueryResult execute(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName must not be blank");
        }

        // Layer 1: city → location (with cache-aside + negative caching)
        Location location = resolveLocation(cityName);

        // Layer 2: location → forecast (with cache-aside)
        String weatherKey = location.weatherCacheKey();
        var cachedForecast = weatherCache.get(weatherKey);
        if (cachedForecast.isPresent()) {
            return new WeatherQueryResult(location, cachedForecast.get());
        }

        WeatherForecast forecast = weatherProvider.getForecast(location);
        weatherCache.put(weatherKey, forecast, weatherCacheTtl);
        return new WeatherQueryResult(location, forecast);
    }

    /**
     * Resolves a city name to a {@link Location} using cache-aside with
     * negative caching:
     * <ol>
     *   <li>Positive cache hit → return.</li>
     *   <li>Negative cache hit → throw {@link LocationNotFoundException}
     *       without an upstream call.</li>
     *   <li>Cache miss on both → call geocoder. On success, write through
     *       (positive entry, long TTL). On miss, mark as absent (negative
     *       entry, short TTL) and throw.</li>
     * </ol>
     */
    private Location resolveLocation(String cityName) {
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

    private static Duration requirePositive(Duration ttl, String name) {
        Objects.requireNonNull(ttl, name);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + ttl);
        }
        return ttl;
    }
}