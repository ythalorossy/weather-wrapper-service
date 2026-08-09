package io.ythalorossy.weatherapi.infrastructure.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.infrastructure.observation.dto.AlertsActiveResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * NWS adapter for {@link AlertProvider}. Calls
 * {@code /alerts/active?point=lat,lon} and maps the GeoJSON FeatureCollection
 * to a list of {@link WeatherAlert}.
 */
@Component
public class NwsAlertProvider implements AlertProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsAlertProvider.class);
    private static final String TIMER_NAME = "weather.provider.nws";

    private final RestClient client;
    private final MeterRegistry meterRegistry;

    public NwsAlertProvider(RestClient nwsRestClient, MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<WeatherAlert> getActiveAlerts(Location location) {
        Objects.requireNonNull(location, "location");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<WeatherAlert> result = doFetch(location);
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "alerts", "outcome", "success"));
            return result;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "alerts", "outcome", "failure"));
            throw e;
        }
    }

    private List<WeatherAlert> doFetch(Location location) {
        AlertsActiveResponse response = invoke(
                () -> client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/alerts/active")
                                .queryParam("point", location.latitude() + "," + location.longitude())
                                .build())
                        .retrieve()
                        .body(AlertsActiveResponse.class),
                "NWS /alerts/active",
                location
        );

        if (response == null || response.features() == null) {
            return List.of();
        }

        List<WeatherAlert> alerts = new ArrayList<>();
        for (AlertsActiveResponse.Feature f : response.features()) {
            if (f.properties() == null) continue;
            try {
                alerts.add(toDomain(f.properties()));
            } catch (DateTimeParseException e) {
                log.warn("Skipping alert with unparseable timestamp: {}", f.properties().id(), e);
            }
        }
        return alerts;
    }

    private static WeatherAlert toDomain(AlertsActiveResponse.Properties p) {
        return new WeatherAlert(
                p.id(),
                p.event(),
                parseSeverity(p.severity()),
                parseCertainty(p.certainty()),
                parseUrgency(p.urgency()),
                parseCategory(p.category()),
                p.headline(),
                p.description(),
                p.instruction(),   // nullable
                p.areaDesc(),
                Instant.parse(p.sent()),
                Instant.parse(p.effective()),
                Instant.parse(p.expires()),
                p.web()             // nullable
        );
    }

    private static AlertSeverity parseSeverity(String s) {
        if (s == null) return AlertSeverity.Unknown;
        try { return AlertSeverity.valueOf(s); }
        catch (IllegalArgumentException e) { return AlertSeverity.Unknown; }
    }

    private static AlertCertainty parseCertainty(String s) {
        if (s == null) return AlertCertainty.Unknown;
        try { return AlertCertainty.valueOf(s); }
        catch (IllegalArgumentException e) { return AlertCertainty.Unknown; }
    }

    private static AlertUrgency parseUrgency(String s) {
        if (s == null) return AlertUrgency.Unknown;
        try { return AlertUrgency.valueOf(s); }
        catch (IllegalArgumentException e) { return AlertUrgency.Unknown; }
    }

    private static AlertCategory parseCategory(String s) {
        if (s == null) return AlertCategory.Unknown;
        try { return AlertCategory.valueOf(s); }
        catch (IllegalArgumentException e) { return AlertCategory.Unknown; }
    }

    private static <T> T invoke(Supplier<T> call, String op, Location location) {
        try {
            return call.get();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new WeatherProviderUnavailableException(
                    op + " returned " + e.getStatusCode() + " for " + location.displayName(), e);
        } catch (Exception e) {
            throw new WeatherProviderUnavailableException(
                    "Failed to call " + op + " for " + location.displayName(), e);
        }
    }
}