package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;

/**
 * REST response for {@code GET /api/v1/weather/metadata?city=...}.
 *
 * <p>Carries the NWS Weather Forecast Office (WFO) info for the resolved
 * city: timezone, radar station, human-readable name, disclaimer URL.
 */
@Schema(description = "Location metadata for a city: timezone, radar station, and NWS Weather Forecast Office info.")
public record LocationMetadataResponse(
        @Schema(description = "City as submitted by the caller (echoed back).",
                example = "Arlington, VA")
        String city,

        @Schema(description = "Coordinates resolved via Nominatim + a human-readable name.")
        WeatherResponse.LocationView resolvedLocation,

        @Schema(description = "NWS Weather Forecast Office that issues forecasts for this location.")
        OfficeView office
) {

    @Schema(description = "NWS Weather Forecast Office (WFO) details.")
    public record OfficeView(
            @Schema(description = "Three-letter WFO identifier.", example = "LWX")
            String officeId,

            @Schema(description = "Human-readable office name.",
                    example = "NWS Baltimore/Washington")
            String name,

            @Schema(description = "Nearest NEXRAD radar station identifier.",
                    example = "KLWX")
            String radarStationId,

            @Schema(description = "IANA timezone id for the location.",
                    example = "America/New_York")
            String timezoneId,

            @Schema(description = "URL to the NWS forecast office page (informational).",
                    example = "https://www.weather.gov/lwx")
            String forecastOfficeUrl,

            @Schema(description = "URL to the NWS disclaimer.",
                    example = "https://www.weather.gov/disclaimer")
            String disclaimerUrl
    ) {
        public static OfficeView from(WeatherOffice office) {
            return new OfficeView(
                    office.officeId(),
                    office.name(),
                    office.radarStationId(),
                    office.timezoneId(),
                    office.disclaimerUrl(),
                    office.forecastOfficeUrl()
            );
        }
    }
}