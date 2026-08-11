package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Use case: active weather alerts at a city.
 *
 * <p>Cache key is the resolved lat/lon (e.g., {@code alerts:38.88,-77.09}).
 * 5-minute TTL — alerts change fast, so a stale "Tornado warning" is a
 * worse UX than a fresh fetch.
 */
public class GetActiveAlertsUseCase {

    private final AlertProvider alertProvider;
    private final Cache<AlertsPayload> alertCache;
    private final LocationResolver locationResolver;
    private final Duration alertCacheTtl;

    public GetActiveAlertsUseCase(
            AlertProvider alertProvider,
            Cache<AlertsPayload> alertCache,
            LocationResolver locationResolver,
            Duration alertCacheTtl) {
        this.alertProvider = Objects.requireNonNull(alertProvider, "alertProvider");
        this.alertCache = Objects.requireNonNull(alertCache, "alertCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        CacheAside.requirePositive(alertCacheTtl, "alertCacheTtl");
        this.alertCacheTtl = alertCacheTtl;
    }

    public List<WeatherAlert> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);

        String key = "%.2f,%.2f".formatted(location.latitude(), location.longitude());
        return CacheAside.getOrLoad(alertCache, key, alertCacheTtl,
                () -> new AlertsPayload(alertProvider.getActiveAlerts(location))).alerts();
    }
}
