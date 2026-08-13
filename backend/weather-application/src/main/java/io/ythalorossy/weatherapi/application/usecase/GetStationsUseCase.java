package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Gridpoint;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.PointsProvider;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class GetStationsUseCase {

    private static final String STATIONS_NS = "stations";
    private static final Duration STATIONS_TTL = Duration.ofHours(1);

    private final LocationResolver locationResolver;
    private final StationsProvider stationsProvider;
    private final PointsProvider pointsProvider;
    private final Cache<List<Station>> stationsCache;

    public GetStationsUseCase(
            LocationResolver locationResolver,
            StationsProvider stationsProvider,
            PointsProvider pointsProvider,
            Cache<List<Station>> stationsCache) {
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        this.stationsProvider = Objects.requireNonNull(stationsProvider, "stationsProvider");
        this.pointsProvider = Objects.requireNonNull(pointsProvider, "pointsProvider");
        this.stationsCache = Objects.requireNonNull(stationsCache, "stationsCache");
    }

    public List<Station> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return CacheAside.getOrLoad(
                stationsCache,
                location.cacheKey(STATIONS_NS).substring(STATIONS_NS.length() + 1),
                STATIONS_TTL,
                () -> {
                    Gridpoint gridpoint = pointsProvider.getGridpoint(location);
                    return stationsProvider.getStations(
                            gridpoint.gridId(), gridpoint.gridX(), gridpoint.gridY());
                });
    }
}
