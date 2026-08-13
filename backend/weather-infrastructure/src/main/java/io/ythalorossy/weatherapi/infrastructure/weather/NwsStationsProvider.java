package io.ythalorossy.weatherapi.infrastructure.weather;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.StationsResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

/**
 * NWS adapter for {@link StationsProvider}.
 *
 * <p>Calls {@code /gridpoints/{gridId}/{x},{y}/stations} and maps the
 * GeoJSON {@code features[].properties} + {@code geometry.coordinates}
 * (always {@code [lon, lat]} per the GeoJSON spec) into domain
 * {@link Station} records.
 */
@Component
public class NwsStationsProvider implements StationsProvider {

    private static final String TIMER_NAME = "weather.provider.nws";

    private final RestClient client;
    private final MeterRegistry meterRegistry;

    public NwsStationsProvider(RestClient nwsRestClient, MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<Station> getStations(String gridId, int gridX, int gridY) {
        Objects.requireNonNull(gridId, "gridId");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Station> stations = doFetch(gridId, gridX, gridY);
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "stations", "outcome", "success"));
            return stations;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "stations", "outcome", "failure"));
            throw e;
        }
    }

    private List<Station> doFetch(String gridId, int gridX, int gridY) {
        Location context = new Location(0.0, 0.0, gridId + "/" + gridX + "," + gridY);
        StationsResponse response = NwsClient.invoke(
                () -> client.get()
                        .uri("/gridpoints/{gridId}/{x},{y}/stations", gridId, gridX, gridY)
                        .retrieve()
                        .body(StationsResponse.class),
                "NWS /gridpoints/.../stations",
                context
        );

        if (response == null || response.features() == null) {
            return List.of();
        }

        return response.features().stream()
                .filter(f -> f.properties() != null && f.properties().stationIdentifier() != null)
                .map(NwsStationsProvider::toDomain)
                .toList();
    }

    private static Station toDomain(StationsResponse.Feature f) {
        var props = f.properties();
        List<Double> coords = f.geometry() == null ? null : f.geometry().coordinates();
        // GeoJSON: coordinates are [longitude, latitude].
        double lon = (coords != null && coords.size() >= 2) ? coords.get(0) : 0.0;
        double lat = (coords != null && coords.size() >= 2) ? coords.get(1) : 0.0;
        return new Station(props.stationIdentifier(), props.name(), lat, lon);
    }
}
