package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.StationObservationProvider;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class GetStationObservationsUseCase {

    private final StationObservationProvider observationProvider;
    private final Cache<List<StationObservation>> observationsCache;
    private final Duration observationsCacheTtl;

    public GetStationObservationsUseCase(
            StationObservationProvider observationProvider,
            Cache<List<StationObservation>> observationsCache,
            Duration observationsCacheTtl) {
        this.observationProvider = Objects.requireNonNull(observationProvider, "observationProvider");
        this.observationsCache = Objects.requireNonNull(observationsCache, "observationsCache");
        this.observationsCacheTtl = Objects.requireNonNull(observationsCacheTtl, "observationsCacheTtl");
    }

    public List<StationObservation> execute(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId must not be blank");
        }
        return CacheAside.getOrLoad(
                observationsCache,
                stationId,
                observationsCacheTtl,
                () -> observationProvider.getObservations(stationId));
    }
}
