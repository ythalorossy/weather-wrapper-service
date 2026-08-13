package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.Station;

import java.util.List;

public interface StationsProvider {

    List<Station> getStations(String gridId, int gridX, int gridY);
}
