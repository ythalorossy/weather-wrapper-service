package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single NWS weather alert. Sourced from {@code /alerts/active} filtered by
 * a point or zone.
 *
 * <p>Severity, certainty, and urgency follow CAP 1.2. The {@code headline} is
 * the human-readable summary, {@code description} is the long form, and
 * {@code instruction} is the recommended action (may be null).
 */
public record WeatherAlert(
        String id,
        String event,
        AlertSeverity severity,
        AlertCertainty certainty,
        AlertUrgency urgency,
        AlertCategory category,
        String headline,
        String description,
        String instruction,
        String areaDesc,
        Instant sent,
        Instant effective,
        Instant expires,
        String webUrl
) {

    public WeatherAlert {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(certainty, "certainty must not be null");
        Objects.requireNonNull(urgency, "urgency must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(headline, "headline must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(areaDesc, "areaDesc must not be null");
        Objects.requireNonNull(sent, "sent must not be null");
        Objects.requireNonNull(effective, "effective must not be null");
        Objects.requireNonNull(expires, "expires must not be null");
        // instruction and webUrl may be null
    }
}