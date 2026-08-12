package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;

import java.time.Duration;
import java.util.Objects;

public class GetHourlyForecastUseCase {

    private static final String HOURLY_NS = "hourly";

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
        this.hourlyCacheTtl = hourlyCacheTtl;
    }

    public HourlyForecast execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return CacheAside.getOrLoad(
                hourlyForecastCache, location.cacheKey(HOURLY_NS).substring(HOURLY_NS.length() + 1), hourlyCacheTtl,
                () -> hourlyWeatherProvider.getHourlyForecast(location)
                        .orElseThrow(() -> new IllegalStateException(
                                "Hourly provider returned empty for " + location.displayName())));
    }
}
