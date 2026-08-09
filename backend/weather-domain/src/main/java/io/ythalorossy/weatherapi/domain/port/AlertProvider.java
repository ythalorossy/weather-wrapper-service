package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;

import java.util.List;

/**
 * Outbound port for active weather alerts at {@code location}. NWS flow:
 * {@code /alerts/active?point=lat,lon} \u2192 list of {@link WeatherAlert}.
 */
public interface AlertProvider {

    /**
     * @return active alerts at the resolved point, in upstream order. Empty
     *         list means no active alerts (not an error).
     */
    List<WeatherAlert> getActiveAlerts(Location location);
}