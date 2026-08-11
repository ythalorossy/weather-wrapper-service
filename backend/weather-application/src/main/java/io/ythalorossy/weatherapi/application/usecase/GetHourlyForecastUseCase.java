package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.HourlyForecastCache;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Cache-aside use case for the hourly forecast endpoint.
 *
 * <p>Resolves the city to a location via the shared {@link LocationResolver},
 * then cache-asides on {@link HourlyForecastCache} (12 h TTL, same cadence as
 * the daily forecast).
 */
public class GetHourlyForecastUseCase {

    private final HourlyWeatherProvider hourlyWeatherProvider;
    private final HourlyForecastCache hourlyForecastCache;
    private final LocationResolver locationResolver;
    private final Duration hourlyCacheTtl;

    public GetHourlyForecastUseCase(
            HourlyWeatherProvider hourlyWeatherProvider,
            HourlyForecastCache hourlyForecastCache,
            LocationResolver locationResolver,
            Duration hourlyCacheTtl) {
        this.hourlyWeatherProvider = Objects.requireNonNull(hourlyWeatherProvider, "hourlyWeatherProvider");
        this.hourlyForecastCache = Objects.requireNonNull(hourlyForecastCache, "hourlyForecastCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.hourlyCacheTtl = requirePositive(hourlyCacheTtl, "hourlyCacheTtl");
    }

    public HourlyForecast execute(String cityName) {
        Location location = locationResolver.resolve(cityName);

        String key = location.cacheKey("hourly");
        var cached = hourlyForecastCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }

        HourlyForecast forecast = hourlyWeatherProvider.getHourlyForecast(location)
                .orElseThrow(() -> new IllegalStateException(
                        "Hourly provider returned empty for " + location.displayName()));
        hourlyForecastCache.put(key, forecast, hourlyCacheTtl);
        return forecast;
    }

    private static Duration requirePositive(Duration ttl, String name) {
        Objects.requireNonNull(ttl, name);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + ttl);
        }
        return ttl;
    }
}