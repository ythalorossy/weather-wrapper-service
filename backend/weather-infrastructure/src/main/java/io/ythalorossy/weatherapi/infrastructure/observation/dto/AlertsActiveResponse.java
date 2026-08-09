package io.ythalorossy.weatherapi.infrastructure.observation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * NWS /alerts/active?point=lat,lon response (internal DTO). GeoJSON FeatureCollection
 * wrapping alert properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertsActiveResponse(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Properties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String id,
            String event,
            @JsonProperty("severity") String severity,
            @JsonProperty("certainty") String certainty,
            @JsonProperty("urgency") String urgency,
            @JsonProperty("category") String category,
            String headline,
            String description,
            String instruction,
            @JsonProperty("areaDesc") String areaDesc,
            String sent,
            String effective,
            String expires,
            String web
    ) {
    }
}