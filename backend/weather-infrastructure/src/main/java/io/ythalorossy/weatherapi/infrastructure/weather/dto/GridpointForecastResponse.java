package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * NWS /gridpoints/{gridId}/{x},{y}/forecast response (internal DTO).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GridpointForecastResponse(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(String generatedAt, List<Period> periods) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Period(
            String name,
            int temperature,
            String temperatureUnit,
            String windSpeed,
            String windDirection,
            String shortForecast,
            String detailedForecast,
            boolean isDaytime
    ) {
    }
}