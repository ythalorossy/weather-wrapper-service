package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;

import java.time.Duration;
import java.util.Objects;

public class GetWeatherUseCase {

    private static final String WEATHER_NS = "weather";

    private final WeatherProvider weatherProvider;
    private final Cache<WeatherForecast> weatherCache;
    private final LocationResolver locationResolver;
    private final Duration weatherCacheTtl;

    public GetWeatherUseCase(
            WeatherProvider weatherProvider,
            Cache<WeatherForecast> weatherCache,
            LocationResolver locationResolver,
            Duration weatherCacheTtl) {
        this.weatherProvider = Objects.requireNonNull(weatherProvider, "weatherProvider");
        this.weatherCache = Objects.requireNonNull(weatherCache, "weatherCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        CacheAside.requirePositive(weatherCacheTtl, "weatherCacheTtl");
        this.weatherCacheTtl = weatherCacheTtl;
    }

    public Location execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        CacheAside.getOrLoad(
                weatherCache, location.cacheKey(WEATHER_NS).substring(WEATHER_NS.length() + 1), weatherCacheTtl,
                () -> weatherProvider.getForecast(location));
        return location;
    }
}
