package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;

import java.util.Optional;

/**
 * Outbound port for the current observation at the nearest NWS observation
 * station to {@code location}. Two-step NWS flow:
 * {@code /points/.../stations} \u2192 nearest station \u2192
 * {@code /stations/{id}/observations/latest}.
 */
public interface ObservationProvider {

    /**
     * @return the latest observation, or empty if no nearby station or the
     *         upstream is unreachable.
     */
    Optional<Observation> getCurrentObservation(Location location);
}