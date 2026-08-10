package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.SunTimesCache;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public class GetSunTimesUseCase {

    private final SunTimesProvider sunTimesProvider;
    private final SunTimesCache sunTimesCache;
    private final LocationResolver locationResolver;
    private final Duration cacheTtl;

    public GetSunTimesUseCase(
            SunTimesProvider sunTimesProvider,
            SunTimesCache sunTimesCache,
            LocationResolver locationResolver,
            Duration cacheTtl) {
        this.sunTimesProvider = Objects.requireNonNull(sunTimesProvider, "sunTimesProvider");
        this.sunTimesCache = Objects.requireNonNull(sunTimesCache, "sunTimesCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.cacheTtl = requirePositive(cacheTtl, "cacheTtl");
    }

    public Optional<SunTimes> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        ZoneId zone = inferZoneFromLocation(location);
        LocalDate today = LocalDate.now(zone);
        String key = String.format("sun:%.2f,%.2f:%s", location.latitude(), location.longitude(), today);

        Optional<SunTimes> cached = sunTimesCache.get(key);
        if (cached.isPresent()) return cached;

        Optional<SunTimes> fresh = sunTimesProvider.getSunTimes(location, today);
        if (fresh.isEmpty()) return Optional.empty();
        sunTimesCache.put(key, fresh.get(), cacheTtl);
        return fresh;
    }

    private ZoneId inferZoneFromLocation(Location location) {
        String name = location.displayName().toLowerCase();
        if (name.contains("arlington") || name.contains("virginia") || name.contains("washington")) {
            return ZoneId.of("America/New_York");
        }
        if (name.contains("honolulu") || name.contains("hawaii")) {
            return ZoneId.of("Pacific/Honolulu");
        }
        return ZoneId.of("UTC");
    }

    private static Duration requirePositive(Duration ttl, String name) {
        Objects.requireNonNull(ttl, name);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + ttl);
        }
        return ttl;
    }
}
