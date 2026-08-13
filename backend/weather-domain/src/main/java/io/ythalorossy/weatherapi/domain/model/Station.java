package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;

public record Station(
        String stationId,
        String name,
        double latitude,
        double longitude
) {

    public Station {
        Objects.requireNonNull(stationId, "stationId must not be null");
        if (stationId.isBlank()) {
            throw new IllegalArgumentException("stationId must not be blank");
        }
    }
}
