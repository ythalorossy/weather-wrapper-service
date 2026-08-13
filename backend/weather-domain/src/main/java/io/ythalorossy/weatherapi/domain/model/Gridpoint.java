package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;

/**
 * NWS gridpoint triple: the office/grid identifier plus the integer
 * x/y coordinates within that grid. Sourced from the NWS
 * {@code /points/{lat},{lon}} response's {@code properties.gridId/gridX/gridY}
 * fields. Required to call any gridpoint-keyed NWS endpoint such as
 * {@code /gridpoints/{gridId}/{x},{y}/stations}.
 */
public record Gridpoint(
        String gridId,
        int gridX,
        int gridY
) {

    public Gridpoint {
        Objects.requireNonNull(gridId, "gridId must not be null");
        if (gridId.isBlank()) {
            throw new IllegalArgumentException("gridId must not be blank");
        }
    }
}
