package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * NWS {@code /gridpoints/{gridId}/{x},{y}/stations} response (internal DTO).
 *
 * <p>Returns observation stations that report weather for the given gridpoint.
 * Coordinates follow GeoJSON convention: {@code [longitude, latitude]}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StationsResponse(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Properties properties, Geometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(String stationIdentifier, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(List<Double> coordinates) {
    }
}
