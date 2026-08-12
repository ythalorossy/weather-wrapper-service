package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class GetActiveAlertsUseCase {

    private static final String ALERTS_NS = "alerts";

    private final AlertProvider alertProvider;
    private final Cache<List<WeatherAlert>> alertCache;
    private final LocationResolver locationResolver;
    private final Duration alertCacheTtl;

    public GetActiveAlertsUseCase(
            AlertProvider alertProvider,
            Cache<List<WeatherAlert>> alertCache,
            LocationResolver locationResolver,
            Duration alertCacheTtl) {
        this.alertProvider = Objects.requireNonNull(alertProvider, "alertProvider");
        this.alertCache = Objects.requireNonNull(alertCache, "alertCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.alertCacheTtl = alertCacheTtl;
    }

    public List<WeatherAlert> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return CacheAside.getOrLoad(
                alertCache, location.cacheKey(ALERTS_NS).substring(ALERTS_NS.length() + 1), alertCacheTtl,
                () -> alertProvider.getActiveAlerts(location));
    }
}
