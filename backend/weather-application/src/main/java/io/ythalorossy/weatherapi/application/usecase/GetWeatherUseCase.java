package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Cache-aside orchestration for the weather query use case.
 *
 * <p>Two-layer cache with negative caching on the geocoding side (handled by
 * the shared {@link LocationResolver}):
 * <ol>
 *   <li>Geocoding cache: city name → {@link Location}, with negative entries
 *       (city not found) cached briefly. Both positive and negative entries
 *       live behind {@code LocationCache}; positive TTL is long (days),
 *       negative TTL is short (seconds-to-minutes).</li>
 *   <li>Weather cache: location → {@link WeatherForecast}. TTL configurable,
 *       default 12 h.</li>
 * </ol>
 *
 * <p>This class is plain Java (no Spring annotations). It is instantiated by a
 * {@code @Configuration} bean in the API module so the application layer stays
 * framework-agnostic.
 */
public class GetWeatherUseCase {

    private final WeatherProvider weatherProvider;
    private final WeatherCache weatherCache;
    private final LocationResolver locationResolver;
    private final Duration weatherCacheTtl;

    public GetWeatherUseCase(
            WeatherProvider weatherProvider,
            WeatherCache weatherCache,
            LocationResolver locationResolver,
            Duration weatherCacheTtl) {
        this.weatherProvider = Objects.requireNonNull(weatherProvider, "weatherProvider");
        this.weatherCache = Objects.requireNonNull(weatherCache, "weatherCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.weatherCacheTtl = requirePositive(weatherCacheTtl, "weatherCacheTtl");
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
        Location location = locationResolver.resolve(cityName);

        String weatherKey = location.weatherCacheKey();
        var cachedForecast = weatherCache.get(weatherKey);
        if (cachedForecast.isPresent()) {
            return new WeatherQueryResult(location, cachedForecast.get());
        }

        WeatherForecast forecast = weatherProvider.getForecast(location);
        weatherCache.put(weatherKey, forecast, weatherCacheTtl);
        return new WeatherQueryResult(location, forecast);
    }

    private static Duration requirePositive(Duration ttl, String name) {
        Objects.requireNonNull(ttl, name);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + ttl);
        }
        return ttl;
    }
}