package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.StationObservation;

import java.util.List;

public interface StationObservationProvider {

    List<StationObservation> getObservations(String stationId);
}
