package io.ythalorossy.weatherapi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;

import java.util.List;

/**
 * REST response for {@code GET /api/v1/alerts?city=...}. List of active NWS
 * weather alerts at the resolved point. Empty list when there are no active
 * alerts (200, not 404).
 */
@Schema(description = "Active weather alerts at the resolved city. Empty list when no alerts are active.")
public record AlertsResponse(
        @Schema(description = "City as submitted by the caller (echoed back).",
                example = "Arlington, VA")
        String city,

        @Schema(description = "Coordinates resolved via Nominatim + a human-readable name.")
        WeatherResponse.LocationView resolvedLocation,

        @Schema(description = "Active alerts at this location, ordered by NWS upstream priority.")
        List<AlertView> alerts
) {

    @Schema(description = "A single NWS weather alert (CAP-IP).")
    public record AlertView(
            @Schema(description = "Unique NWS alert id.",
                    example = "urn:oid:2.49.0.1.840.0.12345678.20260808.123456")
            String id,

            @Schema(description = "Event type, e.g., `Tornado Warning`, `Special Weather Statement`.",
                    example = "Severe Thunderstorm Warning")
            String event,

            @Schema(description = "Severity (CAP-IP).")
            AlertSeverity severity,

            @Schema(description = "Certainty (CAP-IP).")
            AlertCertainty certainty,

            @Schema(description = "Urgency (CAP-IP).")
            AlertUrgency urgency,

            @Schema(description = "Category (CAP-IP).")
            AlertCategory category,

            @Schema(description = "Human-readable one-line headline.")
            String headline,

            @Schema(description = "Long-form description.")
            String description,

            @Schema(description = "Recommended actions. May be null.")
            String instruction,

            @Schema(description = "Human-readable affected area.",
                    example = "Arlington County; City of Alexandria")
            String areaDesc,

            @Schema(description = "ISO-8601 timestamp the alert was sent.",
                    example = "2026-08-08T21:53:00-04:00")
            String sent,

            @Schema(description = "ISO-8601 timestamp the alert takes effect.",
                    example = "2026-08-08T21:53:00-04:00")
            String effective,

            @Schema(description = "ISO-8601 timestamp the alert expires.",
                    example = "2026-08-08T22:30:00-04:00")
            String expires,

            @Schema(description = "URL to the full NWS alert page. May be null.")
            String webUrl
    ) {
        public static AlertView from(WeatherAlert a) {
            return new AlertView(
                    a.id(),
                    a.event(),
                    a.severity(),
                    a.certainty(),
                    a.urgency(),
                    a.category(),
                    a.headline(),
                    a.description(),
                    a.instruction(),
                    a.areaDesc(),
                    a.sent().toString(),
                    a.effective().toString(),
                    a.expires().toString(),
                    a.webUrl()
            );
        }
    }
}