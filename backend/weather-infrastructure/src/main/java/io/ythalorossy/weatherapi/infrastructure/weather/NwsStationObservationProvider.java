package io.ythalorossy.weatherapi.infrastructure.weather;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.port.StationObservationProvider;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.StationObservationsResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * NWS adapter for {@link StationObservationProvider}.
 *
 * <p>Calls {@code /stations/{stationId}/observations} and maps each
 * {@code features[].properties} entry into a domain {@link StationObservation},
 * converting SI measurements (degC, km/h, Pa, percent) to consumer-friendly
 * units (Fahrenheit, mph, inHg).
 */
@Component
public class NwsStationObservationProvider implements StationObservationProvider {

    private static final String TIMER_NAME = "weather.provider.nws";

    private final RestClient client;
    private final MeterRegistry meterRegistry;

    public NwsStationObservationProvider(RestClient nwsRestClient, MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<StationObservation> getObservations(String stationId) {
        Objects.requireNonNull(stationId, "stationId");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<StationObservation> observations = doFetch(stationId);
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "station-observations", "outcome", "success"));
            return observations;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "station-observations", "outcome", "failure"));
            throw e;
        }
    }

    private List<StationObservation> doFetch(String stationId) {
        Location context = new Location(0.0, 0.0, "station " + stationId);
        StationObservationsResponse response = NwsClient.invoke(
                () -> client.get()
                        .uri("/stations/{stationId}/observations", stationId)
                        .retrieve()
                        .body(StationObservationsResponse.class),
                "NWS /stations/.../observations",
                context
        );

        if (response == null || response.features() == null) {
            return List.of();
        }

        return response.features().stream()
                .filter(f -> f.properties() != null)
                .map(f -> toDomain(stationId, f.properties()))
                .toList();
    }

    private static StationObservation toDomain(String stationId,
                                               StationObservationsResponse.Properties p) {
        Instant timestamp;
        try {
            timestamp = Instant.parse(p.timestamp());
        } catch (Exception e) {
            throw new WeatherProviderUnavailableException(
                    "NWS station observation returned unparseable timestamp: " + p.timestamp(), e);
        }

        Integer tempF = roundToInt(celsiusToFahrenheit(valueOf(p.temperature())));
        Temperature temperature = tempF == null ? null : Temperature.fahrenheit(tempF);

        Integer humidity = roundToInt(valueOf(p.relativeHumidity()));

        Double kmh = valueOf(p.windSpeed());
        Double mph = kmh == null ? null : kmh * 0.621371;
        String windSpeed = mph == null ? null : Math.round(mph) + " mph";

        Double deg = valueOf(p.windDirection());
        String windDirection = deg == null ? null : degreesToCompass(deg.intValue());

        Double pa = valueOf(p.barometricPressure());
        Double inHg = pa == null ? null : pa / 3386.39;

        return new StationObservation(
                stationId,
                timestamp,
                temperature,
                humidity,
                windSpeed,
                windDirection,
                p.rawMessage(),
                inHg
        );
    }

    private static Double valueOf(StationObservationsResponse.QuantitativeValue qv) {
        return qv == null ? null : qv.value();
    }

    private static Integer roundToInt(Double v) {
        return v == null ? null : (int) Math.round(v);
    }

    private static Double celsiusToFahrenheit(Double c) {
        return c == null ? null : (c * 9.0 / 5.0) + 32.0;
    }

    private static String degreesToCompass(int degrees) {
        String[] dirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                         "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int idx = (int) Math.round(((degrees % 360) / 22.5)) % 16;
        return dirs[idx];
    }
}
