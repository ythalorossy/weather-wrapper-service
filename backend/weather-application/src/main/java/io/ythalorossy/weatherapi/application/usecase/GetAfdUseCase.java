package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case that returns the latest Area Forecast Discussion (AFD) for a city.
 *
 * <p>Resolves the city via {@link LocationResolver} (bubbles up
 * "not found"), then asks {@link LocationMetadataProvider} for the issuing
 * office. Cache-aside on the office id with the configured TTL.
 *
 * <p>Both "no office for this city" (overseas / non-US) and "no AFD for
 * this office" return {@code Optional.empty()}. The controller maps that
 * to a 404.
 */
public class GetAfdUseCase {

    private static final String AFD_KEY_PREFIX = "afd:";

    private final AreaForecastDiscussionProvider provider;
    private final Cache<AfdProduct> cache;
    private final LocationResolver locationResolver;
    private final LocationMetadataProvider metadataProvider;
    private final Duration cacheTtl;

    public GetAfdUseCase(
            AreaForecastDiscussionProvider provider,
            Cache<AfdProduct> cache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            Duration cacheTtl) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        this.cacheTtl = cacheTtl;
    }

    public Optional<AfdProduct> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        Optional<WeatherOffice> office = metadataProvider.getOfficeFor(location);
        if (office.isEmpty()) {
            return Optional.empty();
        }
        String officeId = office.get().officeId();
        String cacheKey = ("afd:" + officeId).substring(AFD_KEY_PREFIX.length());

        Optional<AfdProduct> cached = cache.get(cacheKey);
        if (cached.isPresent()) return cached;

        Optional<AfdProduct> fresh = provider.getLatest(officeId);
        if (fresh.isEmpty()) return Optional.empty();
        cache.put(cacheKey, fresh.get(), cacheTtl);
        return fresh;
    }
}