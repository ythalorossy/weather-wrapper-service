package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * NWS {@code /stations/{stationId}/observations} response (internal DTO).
 *
 * <p>Returns recent observations from the station (typically the last 24\u201348 h).
 * Measurement values are wrapped in {@link QuantitativeValue} with a
 * {@code unitCode} field (e.g. {@code wmoUnit:degC}, {@code wmoUnit:Pa}); the
 * adapter converts to consumer-friendly units before building the domain
 * {@code StationObservation}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StationObservationsResponse(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Properties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String timestamp,
            String rawMessage,
            QuantitativeValue temperature,
            QuantitativeValue windDirection,
            QuantitativeValue windSpeed,
            QuantitativeValue barometricPressure,
            QuantitativeValue relativeHumidity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuantitativeValue(
            Double value,
            @JsonProperty("unitCode") String unitCode
    ) {
    }
}
