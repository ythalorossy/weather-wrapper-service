package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public class GetSunTimesUseCase {

    private static final String SUN_NS = "sun";

    private final SunTimesProvider sunTimesProvider;
    private final Cache<SunTimes> sunTimesCache;
    private final LocationResolver locationResolver;
    private final LocationMetadataProvider metadataProvider;
    private final Duration cacheTtl;

    public GetSunTimesUseCase(
            SunTimesProvider sunTimesProvider,
            Cache<SunTimes> sunTimesCache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            Duration cacheTtl) {
        this.sunTimesProvider = Objects.requireNonNull(sunTimesProvider, "sunTimesProvider");
        this.sunTimesCache = Objects.requireNonNull(sunTimesCache, "sunTimesCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        CacheAside.requirePositive(cacheTtl, "cacheTtl");
        this.cacheTtl = cacheTtl;
    }

    public Optional<SunTimes> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        ZoneId zone = metadataProvider.getOfficeFor(location)
                .map(o -> ZoneId.of(o.timezoneId()))
                .orElse(ZoneId.of("UTC"));
        LocalDate today = LocalDate.now(zone);
        String locationKey = location.cacheKey(SUN_NS).substring(SUN_NS.length() + 1);
        String key = locationKey + ":" + today;

        Optional<SunTimes> cached = sunTimesCache.get(key);
        if (cached.isPresent()) return cached;

        Optional<SunTimes> fresh = sunTimesProvider.getSunTimes(location, today, zone);
        fresh.ifPresent(s -> sunTimesCache.put(key, s, cacheTtl));
        return fresh;
    }
}
