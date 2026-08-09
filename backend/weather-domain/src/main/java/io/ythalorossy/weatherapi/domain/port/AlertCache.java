package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.WeatherAlert;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cache port for active weather alerts at a location.
 * Keyed by {@code alerts:{lat:.2f},{lon:.2f}}. 5-minute TTL (alerts change
 * fast \u2014 short TTL prevents stale warnings).
 */
public interface AlertCache {

    Optional<List<WeatherAlert>> get(String key);

    void put(String key, List<WeatherAlert> alerts, Duration ttl);
}