package io.ythalorossy.weatherapi.domain.model;

/**
 * NWS CAP-IP urgency levels \u2014 how soon the event will happen.
 */
public enum AlertUrgency {
    Immediate,
    Expected,
    Future,
    Past,
    Unknown
}