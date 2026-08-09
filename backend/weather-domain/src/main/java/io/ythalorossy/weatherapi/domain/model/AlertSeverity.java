package io.ythalorossy.weatherapi.domain.model;

/**
 * NWS CAP-IP severity levels. See
 * <a href="https://docs.oasis-open.org/emergency/cap/v1.2/CAP-v1.2-os.html">CAP 1.2</a>.
 */
public enum AlertSeverity {
    Extreme,
    Severe,
    Moderate,
    Minor,
    Unknown
}