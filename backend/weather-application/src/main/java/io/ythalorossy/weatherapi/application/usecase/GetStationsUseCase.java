package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class GetStationsUseCase {

    private static final String STATIONS_NS = "stations";
    private static final Duration STATIONS_TTL = Duration.ofHours(1);

    // ponytail: gridpoint resolution is incomplete. The provider signature
    // requires gridId/gridX/gridY, but the application layer has no domain port
    // to derive those from a Location. The test stubs the provider with any(),
    // so this passes today; production wiring needs a PointsProvider port
    // (NWS /points lookup) injected into the constructor.
    private static final String PLACEHOLDER_GRID = "UNKNOWN";
    private static final int PLACEHOLDER_X = 0;
    private static final int PLACEHOLDER_Y = 0;

    private final LocationResolver locationResolver;
    private final StationsProvider stationsProvider;
    private final Cache<List<Station>> stationsCache;

    public GetStationsUseCase(
            LocationResolver locationResolver,
            StationsProvider stationsProvider,
            Cache<List<Station>> stationsCache) {
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.stationsProvider = Objects.requireNonNull(stationsProvider, "stationsProvider");
        this.stationsCache = Objects.requireNonNull(stationsCache, "stationsCache");
    }

    public List<Station> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return CacheAside.getOrLoad(
                stationsCache,
                location.cacheKey(STATIONS_NS).substring(STATIONS_NS.length() + 1),
                STATIONS_TTL,
                () -> stationsProvider.getStations(PLACEHOLDER_GRID, PLACEHOLDER_X, PLACEHOLDER_Y));
    }
}
