package io.ythalorossy.weatherapi.infrastructure.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;
import io.ythalorossy.weatherapi.infrastructure.observation.dto.GridpointStationsResponse;
import io.ythalorossy.weatherapi.infrastructure.observation.dto.ObservationLatestResponse;
import io.ythalorossy.weatherapi.infrastructure.weather.NwsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * NWS adapter for {@link ObservationProvider}.
 *
 * <p>Three-step flow: {@code /points/{lat},{lon}} (already used by the
 * forecast path) is skipped here because we can hit
 * {@code /points/{lat},{lon}/stations} directly \u2014 it redirects to the
 * gridpoint's stations endpoint internally. Then we fetch the latest
 * observation from the nearest station.
 */
@Component
public class NwsObservationProvider implements ObservationProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsObservationProvider.class);
    private static final String TIMER_NAME = "weather.provider.nws";

    private final RestClient client;
    private final MeterRegistry meterRegistry;

    public NwsObservationProvider(RestClient nwsRestClient, MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<Observation> getCurrentObservation(Location location) {
        Objects.requireNonNull(location, "location");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Optional<Observation> result = doFetch(location);
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "observations", "outcome", "success"));
            return result;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "observations", "outcome", "failure"));
            throw e;
        }
    }

    private Optional<Observation> doFetch(Location location) {
        // Step 1: find nearby stations (sorted by distance; first is nearest).
        GridpointStationsResponse stations = NwsClient.invoke(
                () -> client.get()
                        .uri("/points/{lat},{lon}/stations", location.latitude(), location.longitude())
                        .retrieve()
                        .body(GridpointStationsResponse.class),
                "NWS /points/.../stations",
                location
        );

        if (stations == null || stations.features() == null || stations.features().isEmpty()) {
            log.debug("No nearby stations for {} ({},{})", location.displayName(),
                    location.latitude(), location.longitude());
            return Optional.empty();
        }

        GridpointStationsResponse.Feature nearest = stations.features().get(0);
        String stationId = nearest.properties().stationIdentifier();
        String stationName = nearest.properties().name();
        if (stationId == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points/.../stations returned no stationIdentifier for " + location.displayName());
        }

        // Step 2: fetch latest observation from the nearest station.
        ObservationLatestResponse obs = NwsClient.invoke(
                () -> client.get()
                        .uri("/stations/{stationId}/observations/latest", stationId)
                        .retrieve()
                        .body(ObservationLatestResponse.class),
                "NWS /stations/.../observations/latest",
                location
        );

        if (obs == null || obs.properties() == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /stations/" + stationId + "/observations/latest returned empty body");
        }

        return Optional.of(toDomain(obs.properties(), stationId, stationName));
    }

    private static Observation toDomain(ObservationLatestResponse.Properties p,
                                       String stationId, String stationName) {
        Instant timestamp;
        try {
            timestamp = Instant.parse(p.timestamp());
        } catch (Exception e) {
            throw new WeatherProviderUnavailableException(
                    "NWS observation returned unparseable timestamp: " + p.timestamp(), e);
        }

        Double tempF = celsiusToFahrenheit(p.temperature() == null ? null : p.temperature().value());
        Double dewF  = celsiusToFahrenheit(p.dewpoint()    == null ? null : p.dewpoint().value());
        Double windMph = kmhToMph(p.windSpeed() == null ? null : p.windSpeed().value());
        Double humidity = p.relativeHumidity() == null ? null : p.relativeHumidity().value();
        Double pressureInHg = paToInHg(p.barometricPressure() == null ? null : p.barometricPressure().value());
        Integer windDir = p.windDirection() == null ? null : p.windDirection().value() == null ? null
                : (int) Math.round(p.windDirection().value());

        return new Observation(
                stationId,
                stationName,
                timestamp,
                tempF,
                dewF,
                windMph,
                windDir,
                windDir == null ? null : degreesToCompass(windDir),
                humidity,
                pressureInHg,
                p.textDescription()
        );
    }

    private static Double celsiusToFahrenheit(Double c) {
        return c == null ? null : (c * 9.0 / 5.0) + 32.0;
    }

    private static Double kmhToMph(Double kmh) {
        return kmh == null ? null : kmh * 0.621371;
    }

    /** Pa to inches of mercury (US weather convention). */
    private static Double paToInHg(Double pa) {
        return pa == null ? null : pa / 3386.39;
    }

    private static String degreesToCompass(int degrees) {
        String[] dirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                         "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int idx = (int) Math.round(((degrees % 360) / 22.5)) % 16;
        return dirs[idx];
    }
}