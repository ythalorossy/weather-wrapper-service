package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
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
    private final LocationMetadataProvider metadataProvider;
    private final Duration cacheTtl;

    public GetSunTimesUseCase(
            SunTimesProvider sunTimesProvider,
            SunTimesCache sunTimesCache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            Duration cacheTtl) {
        this.sunTimesProvider = Objects.requireNonNull(sunTimesProvider, "sunTimesProvider");
        this.sunTimesCache = Objects.requireNonNull(sunTimesCache, "sunTimesCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        this.cacheTtl = requirePositive(cacheTtl, "cacheTtl");
    }

    public Optional<SunTimes> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        ZoneId zone = metadataProvider.getOfficeFor(location)
                .map(o -> ZoneId.of(o.timezoneId()))
                .orElse(ZoneId.of("UTC"));
        LocalDate today = LocalDate.now(zone);
        String key = location.cacheKey("sun") + ":" + today;

        Optional<SunTimes> cached = sunTimesCache.get(key);
        if (cached.isPresent()) return cached;

        Optional<SunTimes> fresh = sunTimesProvider.getSunTimes(location, today, zone);
        if (fresh.isEmpty()) return Optional.empty();
        sunTimesCache.put(key, fresh.get(), cacheTtl);
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
