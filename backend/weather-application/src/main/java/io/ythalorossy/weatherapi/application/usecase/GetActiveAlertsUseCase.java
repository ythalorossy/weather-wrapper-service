package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.port.AlertCache;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Use case: active weather alerts at a city.
 *
 * <p>Cache key is the resolved lat/lon (e.g., {@code alerts:38.88,-77.09}).
 * 5-minute TTL \u2014 alerts change fast, so a stale "Tornado warning" is a
 * worse UX than a fresh fetch.
 */
public class GetActiveAlertsUseCase {

    private final AlertProvider alertProvider;
    private final AlertCache alertCache;
    private final LocationResolver locationResolver;
    private final Duration alertCacheTtl;

    public GetActiveAlertsUseCase(
            AlertProvider alertProvider,
            AlertCache alertCache,
            LocationResolver locationResolver,
            Duration alertCacheTtl) {
        this.alertProvider = Objects.requireNonNull(alertProvider, "alertProvider");
        this.alertCache = Objects.requireNonNull(alertCache, "alertCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.alertCacheTtl = requirePositive(alertCacheTtl, "alertCacheTtl");
    }

    public List<WeatherAlert> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);

        String key = "alerts:" + String.format("%.2f,%.2f", location.latitude(), location.longitude());
        var cached = alertCache.get(key);
        if (cached.isPresent()) return cached.get();

        List<WeatherAlert> fresh = alertProvider.getActiveAlerts(location);
        alertCache.put(key, fresh, alertCacheTtl);
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