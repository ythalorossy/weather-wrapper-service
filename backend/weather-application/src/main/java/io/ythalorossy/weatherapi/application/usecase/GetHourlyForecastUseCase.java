package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Cache-aside use case for the hourly forecast endpoint.
 *
 * <p>Resolves the city to a location via the shared {@link LocationResolver},
 * then cache-asides on {@link Cache} (12 h TTL, same cadence as the daily
 * forecast).
 */
public class GetHourlyForecastUseCase {

    private static final String HOURLY_KEY_PREFIX = "hourly:";

    private final HourlyWeatherProvider hourlyWeatherProvider;
    private final Cache<HourlyForecast> hourlyForecastCache;
    private final LocationResolver locationResolver;
    private final Duration hourlyCacheTtl;

    public GetHourlyForecastUseCase(
            HourlyWeatherProvider hourlyWeatherProvider,
            Cache<HourlyForecast> hourlyForecastCache,
            LocationResolver locationResolver,
            Duration hourlyCacheTtl) {
        this.hourlyWeatherProvider = Objects.requireNonNull(hourlyWeatherProvider, "hourlyWeatherProvider");
        this.hourlyForecastCache = Objects.requireNonNull(hourlyForecastCache, "hourlyForecastCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        CacheAside.requirePositive(hourlyCacheTtl, "hourlyCacheTtl");
        this.hourlyCacheTtl = hourlyCacheTtl;
    }

    public HourlyForecast execute(String cityName) {
        Location location = locationResolver.resolve(cityName);

        String hourlyKey = location.hourlyCacheKey()
                .substring(HOURLY_KEY_PREFIX.length());
        return CacheAside.getOrLoad(
                hourlyForecastCache, hourlyKey, hourlyCacheTtl,
                () -> hourlyWeatherProvider.getHourlyForecast(location)
                        .orElseThrow(() -> new IllegalStateException(
                                "Hourly provider returned empty for " + location.displayName())));
    }
}