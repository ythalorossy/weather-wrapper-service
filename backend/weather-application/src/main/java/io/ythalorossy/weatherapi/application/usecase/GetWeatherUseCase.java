package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Cache-aside orchestration for the weather query use case.
 *
 * <p>Flow:
 * <ol>
 *   <li>Resolve the user-entered city name to a {@link Location} via {@link GeocodingProvider}.</li>
 *   <li>Look up the cache (key derived from {@link Location#weatherCacheKey()}).</li>
 *   <li>On hit, return cached forecast.</li>
 *   <li>On miss, fetch from upstream {@link WeatherProvider} and write through to {@link WeatherCache}.</li>
 * </ol>
 *
 * <p>This class is plain Java (no Spring annotations). It is instantiated by a
 * {@code @Configuration} bean in the API module so the application layer stays
 * framework-agnostic.
 */
public class GetWeatherUseCase {

    private final GeocodingProvider geocodingProvider;
    private final WeatherProvider weatherProvider;
    private final WeatherCache cache;
    private final Duration cacheTtl;

    public GetWeatherUseCase(
            GeocodingProvider geocodingProvider,
            WeatherProvider weatherProvider,
            WeatherCache cache,
            Duration cacheTtl) {
        this.geocodingProvider = Objects.requireNonNull(geocodingProvider, "geocodingProvider");
        this.weatherProvider = Objects.requireNonNull(weatherProvider, "weatherProvider");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl");
        if (cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must be positive: " + cacheTtl);
        }
    }

    /**
     * Executes the weather query for the given city name.
     *
     * @param cityName user-entered city (e.g., "Arlington, VA"); must not be null or blank
     * @return the resolved location plus the forecast (cached or freshly fetched)
     * @throws LocationNotFoundException     if the city cannot be resolved to a location
     * @throws IllegalArgumentException      if {@code cityName} is null or blank
     */
    public WeatherQueryResult execute(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName must not be blank");
        }

        // 1. Resolve city → location
        Location location = geocodingProvider.findLocation(cityName)
                .orElseThrow(() -> new LocationNotFoundException(cityName));

        // 2. Cache lookup (key derived from the location, not the city name)
        String key = location.weatherCacheKey();
        var cached = cache.get(key);
        if (cached.isPresent()) {
            return new WeatherQueryResult(location, cached.get());
        }

        // 3. Cache miss → upstream fetch
        WeatherForecast forecast = weatherProvider.getForecast(location);

        // 4. Write through to cache
        cache.put(key, forecast, cacheTtl);

        return new WeatherQueryResult(location, forecast);
    }
}