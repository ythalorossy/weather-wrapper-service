package io.ythalorossy.weatherapi.infrastructure.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * NWS /offices/{officeId} response (internal DTO). Provides the human-readable
 * WFO name and the {@code sameAs} URL (the WFO's web page, e.g.,
 * {@code https://www.weather.gov/lwx}).
 *
 * <p>The full response also includes address, telephone, email, responsible
 * counties/zones, and approved observation stations; we only need name + the
 * public page URL for now. {@link JsonIgnoreProperties} lets us deserialize
 * without mapping them all.
 *
 * <p>There is no per-office disclaimer URL on this endpoint — the NWS
 * disclaimer is a global page ({@code https://www.weather.gov/disclaimer}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OfficeResponse(
        String name,
        @JsonProperty("sameAs") String sameAs
) {
}