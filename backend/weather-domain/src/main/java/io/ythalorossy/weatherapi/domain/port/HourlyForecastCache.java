package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.HourlyForecast;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache port for hourly forecasts. Sibling to {@link WeatherCache} (12-hour
 * blocks). Same key strategy: {@code hourly:{lat:.2f},{lon:.2f}} so trivial
 * coordinate variations share a slot.
 */
public interface HourlyForecastCache {

    Optional<HourlyForecast> get(String key);

    void put(String key, HourlyForecast forecast, Duration ttl);
}