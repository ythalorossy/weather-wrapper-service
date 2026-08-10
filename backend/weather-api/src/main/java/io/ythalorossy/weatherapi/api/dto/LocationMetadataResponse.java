package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * REST response for {@code GET /api/v1/weather/metadata?city=...}.
 *
 * <p>Carries the NWS Weather Forecast Office (WFO) info for the resolved
 * city: timezone, radar station, human-readable name, disclaimer URL.
 * When sunrise/sunset computation is available, the {@link SunView} sub-view
 * is populated; otherwise it's omitted from the JSON entirely.
 */
@Schema(description = "Location metadata for a city: timezone, radar station, NWS Weather Forecast Office info, and today's sunrise/sunset.")
public record LocationMetadataResponse(
        @Schema(description = "City as submitted by the caller (echoed back).",
                example = "Arlington, VA")
        String city,

        @Schema(description = "Coordinates resolved via Nominatim + a human-readable name.")
        WeatherResponse.LocationView resolvedLocation,

        @Schema(description = "NWS Weather Forecast Office that issues forecasts for this location.")
        OfficeView office,

        @Schema(description = "Today's sunrise and sunset for the resolved location. Absent (null in JSON) on polar edge or when computation failed.",
                nullable = true)
        SunView sun
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

            @Schema(description = "URL to the NWS forecast office page (e.g., https://www.weather.gov/lwx).",
                    example = "https://www.weather.gov/lwx")
            String forecastOfficeUrl
    ) {
        public static OfficeView from(WeatherOffice office) {
            return new OfficeView(
                    office.officeId(),
                    office.name(),
                    office.radarStationId(),
                    office.timezoneId(),
                    office.forecastOfficeUrl()
            );
        }
    }

    @Schema(description = "Sunrise/sunset for the resolved location on the date listed.")
    public record SunView(
            @Schema(description = "Date the sun times apply to, in the location's timezone.",
                    example = "2026-08-09")
            LocalDate date,

            @Schema(description = "Sunrise in HH:mm, formatted in the location's timezone.",
                    example = "06:42")
            String sunriseLocal,

            @Schema(description = "Sunset in HH:mm, formatted in the location's timezone.",
                    example = "19:34")
            String sunsetLocal,

            @Schema(description = "Day length in seconds. UI can format as '13h 52m'.",
                    example = "48720")
            long dayLengthSeconds
    ) {
        private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

        public static SunView from(SunTimes times) {
            ZoneId zone = ZoneId.of(times.timezoneId());
            String sunriseLocal = HHMM.format(times.sunrise().atZone(zone));
            String sunsetLocal = HHMM.format(times.sunset().atZone(zone));
            long dayLengthSeconds = times.sunset().getEpochSecond() - times.sunrise().getEpochSecond();
            return new SunView(times.date(), sunriseLocal, sunsetLocal, dayLengthSeconds);
        }
    }
}
