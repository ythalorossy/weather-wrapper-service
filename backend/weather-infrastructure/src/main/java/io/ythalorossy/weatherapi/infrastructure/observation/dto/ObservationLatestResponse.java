package io.ythalorossy.weatherapi.infrastructure.observation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * NWS /stations/{stationId}/observations/latest response (internal DTO).
 *
 * <p>Observation values come in SI units (degC, km/h, Pa, percent) with a
 * {@code unitCode} field. The adapter converts to consumer-friendly units
 * (Fahrenheit, mph, inHg) before building the domain {@code Observation}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ObservationLatestResponse(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String timestamp,
            String textDescription,
            QuantitativeValue temperature,
            QuantitativeValue dewpoint,
            QuantitativeValue windDirection,
            QuantitativeValue windSpeed,
            QuantitativeValue barometricPressure,
            QuantitativeValue relativeHumidity,
            QuantitativeValue windChill,
            QuantitativeValue heatIndex
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuantitativeValue(
            Double value,
            @JsonProperty("unitCode") String unitCode
    ) {
    }
}