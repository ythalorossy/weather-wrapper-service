package io.ythalorossy.weatherapi.domain.model;

import java.util.Objects;

/**
 * NWS Weather Forecast Office (WFO) metadata for a location.
 *
 * <p>Source: NWS {@code /points} response's {@code properties.cwa} +
 * {@code /offices/{officeId}} response. Each WFO is responsible for issuing
 * forecasts for a specific region (e.g., KLWX = Baltimore/Washington).
 */
public record WeatherOffice(
        String officeId,
        String name,
        String radarStationId,
        String timezoneId,
        String disclaimerUrl,
        String forecastOfficeUrl
) {

    public WeatherOffice {
        Objects.requireNonNull(officeId, "officeId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(radarStationId, "radarStationId must not be null");
        Objects.requireNonNull(timezoneId, "timezoneId must not be null");
        Objects.requireNonNull(disclaimerUrl, "disclaimerUrl must not be null");
        Objects.requireNonNull(forecastOfficeUrl, "forecastOfficeUrl must not be null");
    }
}