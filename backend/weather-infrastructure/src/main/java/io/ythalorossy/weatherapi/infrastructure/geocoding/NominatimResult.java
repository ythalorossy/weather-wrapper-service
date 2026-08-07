package io.ythalorossy.weatherapi.infrastructure.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nominatim search result (internal DTO).
 *
 * <p>Nominatim returns {@code lat} and {@lon} as strings — they must be parsed.
 * Unrecognized fields are ignored to keep forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResult(
        @JsonProperty("lat") String lat,
        @JsonProperty("lon") String lon,
        @JsonProperty("display_name") String displayName
) {
}