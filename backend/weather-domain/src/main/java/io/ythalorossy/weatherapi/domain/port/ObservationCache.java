package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Observation;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache port for the latest observation at a given station.
 * Keyed by station id (e.g., {@code obs:KIAD}). 10-minute TTL.
 */
public interface ObservationCache {

    Optional<Observation> get(String key);

    void put(String key, Observation observation, Duration ttl);
}