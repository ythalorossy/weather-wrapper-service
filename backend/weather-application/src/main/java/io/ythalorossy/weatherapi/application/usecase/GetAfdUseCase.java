package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.AfdCache;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
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

    private final AreaForecastDiscussionProvider provider;
    private final AfdCache cache;
    private final LocationResolver locationResolver;
    private final LocationMetadataProvider metadataProvider;
    private final Duration cacheTtl;

    public GetAfdUseCase(
            AreaForecastDiscussionProvider provider,
            AfdCache cache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            Duration cacheTtl) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
        this.cacheTtl = requirePositive(cacheTtl, "cacheTtl");
    }

    public Optional<AfdProduct> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        Optional<WeatherOffice> office = metadataProvider.getOfficeFor(location);
        if (office.isEmpty()) {
            return Optional.empty();
        }
        String key = "afd:" + office.get().officeId();

        Optional<AfdProduct> cached = cache.get(key);
        if (cached.isPresent()) return cached;

        Optional<AfdProduct> fresh = provider.getLatest(office.get().officeId());
        if (fresh.isEmpty()) return Optional.empty();
        cache.put(key, fresh.get(), cacheTtl);
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
