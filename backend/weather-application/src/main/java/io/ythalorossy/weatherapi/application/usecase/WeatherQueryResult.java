package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;

/**
 * Result of a {@link GetWeatherUseCase} invocation: the resolved location
 * plus the forecast served for it. The controller uses both pieces to shape
 * the HTTP response.
 */
public record WeatherQueryResult(
        Location location,
        WeatherForecast forecast
) {
}