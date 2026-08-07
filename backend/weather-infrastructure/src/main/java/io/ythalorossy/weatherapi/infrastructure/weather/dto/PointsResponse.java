package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * NWS /points response (internal DTO). We only need the gridpoint triple.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PointsResponse(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(String gridId, int gridX, int gridY) {
    }
}