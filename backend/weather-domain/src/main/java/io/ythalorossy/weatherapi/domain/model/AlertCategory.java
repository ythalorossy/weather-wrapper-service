package io.ythalorossy.weatherapi.domain.model;

/**
 * NWS alert categories. Most consumer-facing alerts use {@link #Met}
 * (meteorological), but Health/Air-Quality alerts are common in some regions.
 */
public enum AlertCategory {
    Met,
    Health,
    Security,
    Safety,
    Hydrological,
    Marine,
    Fire,
    Quality,
    Aviation,
    Law,
    Unknown
}