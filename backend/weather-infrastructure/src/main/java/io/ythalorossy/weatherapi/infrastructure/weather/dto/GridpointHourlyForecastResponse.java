package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * NWS /gridpoints/{gridId}/{x},{y}/forecast/hourly response (internal DTO).
 *
 * <p>Shape mirrors {@link GridpointForecastResponse} except each period has an
 * explicit {@code startTime} and no {@code name} / {@code detailedForecast}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GridpointHourlyForecastResponse(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(String generatedAt, List<Period> periods) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Period(
            String startTime,
            int temperature,
            String temperatureUnit,
            String windSpeed,
            String windDirection,
            String shortForecast,
            boolean isDaytime
    ) {
    }
}