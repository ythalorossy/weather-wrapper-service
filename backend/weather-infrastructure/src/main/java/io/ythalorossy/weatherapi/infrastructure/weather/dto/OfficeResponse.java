package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * NWS /offices/{officeId} response (internal DTO). Provides the human-readable
 * WFO name and disclaimer URL that we want to surface to clients.
 *
 * <p>The full response also includes address, telephone, email, and serving
 * counties; we only need name + disclaimerUrl for now.
 * {@link JsonIgnoreProperties} lets us deserialize without mapping them all.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OfficeResponse(
        String name,
        @JsonProperty("disclaimerUrl") String disclaimerUrl
) {
}