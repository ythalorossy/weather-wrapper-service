package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;

import java.util.Optional;

/**
 * Outbound port for hourly-resolution weather forecasts. Sibling to
 * {@link WeatherProvider} which carries 12-hour blocks.
 */
public interface HourlyWeatherProvider {

    /**
     * Returns the hourly forecast for {@code location}, or empty if upstream
     * could not be reached or returned no usable data.
     */
    Optional<HourlyForecast> getHourlyForecast(Location location);
}