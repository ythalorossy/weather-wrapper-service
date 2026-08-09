package io.ythalorossy.weatherapi.domain.model;

/**
 * NWS CAP-IP certainty levels \u2014 how likely the event is to occur.
 */
public enum AlertCertainty {
    Observed,
    Likely,
    Possible,
    Unlikely,
    Unknown
}