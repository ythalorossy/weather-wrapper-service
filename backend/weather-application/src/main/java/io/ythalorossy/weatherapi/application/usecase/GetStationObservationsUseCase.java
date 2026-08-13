package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.StationObservationProvider;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class GetStationObservationsUseCase {

    private static final Duration OBSERVATIONS_TTL = Duration.ofMinutes(10);

    private final StationObservationProvider observationProvider;
    private final Cache<List<StationObservation>> observationsCache;

    public GetStationObservationsUseCase(
            StationObservationProvider observationProvider,
            Cache<List<StationObservation>> observationsCache) {
        this.observationProvider = Objects.requireNonNull(observationProvider, "observationProvider");
        this.observationsCache = Objects.requireNonNull(observationsCache, "observationsCache");
    }

    public List<StationObservation> execute(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId must not be blank");
        }
        return CacheAside.getOrLoad(
                observationsCache,
                stationId,
                OBSERVATIONS_TTL,
                () -> observationProvider.getObservations(stationId));
    }
}
