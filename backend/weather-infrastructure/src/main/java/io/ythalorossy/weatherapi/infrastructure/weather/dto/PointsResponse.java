package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * NWS /points response (internal DTO). We need the gridpoint triple plus the
 * forecast-office, time-zone, and radar-station references for metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PointsResponse(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String gridId,
            int gridX,
            int gridY,
            String timeZone,
            String radarStation,
            String forecastOffice
    ) {
    }
}