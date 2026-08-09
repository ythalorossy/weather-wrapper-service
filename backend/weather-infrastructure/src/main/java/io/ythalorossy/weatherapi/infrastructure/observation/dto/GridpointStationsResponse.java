package io.ythalorossy.weatherapi.infrastructure.observation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * NWS /points/{lat},{lon}/stations response (internal DTO). Returns nearby
 * observation stations sorted by distance from the gridpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GridpointStationsResponse(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Properties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String stationIdentifier,
            String name,
            Double latitude,
            Double longitude,
            Double elevation,
            Double distance  // meters from the gridpoint
    ) {
    }
}