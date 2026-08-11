package io.ythalorossy.weatherapi.infrastructure.weather;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.OfficeResponse;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * NWS adapter for {@link LocationMetadataProvider}.
 *
 * <p>Two-step: {@code /points/{lat},{lon}} → {@code forecastOffice} URL and
 * timezone, then {@code /offices/{officeId}} → human-readable name + disclaimer.
 */
@Component
public class NwsLocationMetadataProvider implements LocationMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsLocationMetadataProvider.class);
    private static final String TIMER_NAME = "weather.provider.nws";
    /** Suffix used to derive the officeId from a forecastOffice URL. */
    private static final String OFFICE_PATH = "/offices/";

    private final RestClient client;
    private final NwsPointsService pointsService;
    private final MeterRegistry meterRegistry;

    public NwsLocationMetadataProvider(RestClient nwsRestClient,
                                       NwsPointsService pointsService,
                                       MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.pointsService = pointsService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<WeatherOffice> getOfficeFor(Location location) {
        Objects.requireNonNull(location, "location");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            WeatherOffice office = doFetch(location);
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "metadata", "outcome", "success"));
            return Optional.of(office);
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "metadata", "outcome", "failure"));
            throw e;
        }
    }

    private WeatherOffice doFetch(Location location) {
        // Step 1: lat/lon → office URL (cached per coordinate)
        PointsResponse points = pointsService.lookup(location.latitude(), location.longitude());

        if (points == null || points.properties() == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned empty body for " + location.displayName());
        }

        var props = points.properties();
        String officeId = parseOfficeId(props.forecastOffice());
        if (officeId == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned no usable forecastOffice URL for " + location.displayName());
        }
        if (props.timeZone() == null || props.radarStation() == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned missing timeZone or radarStation for " + location.displayName());
        }

        // Step 2: office details (name + disclaimer)
        OfficeResponse office = invoke(
                () -> client.get()
                        .uri("/offices/{officeId}", officeId)
                        .retrieve()
                        .body(OfficeResponse.class),
                "NWS /offices",
                location
        );

        if (office == null || office.name() == null || office.sameAs() == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /offices/" + officeId + " returned empty body for " + location.displayName());
        }

        return new WeatherOffice(
                officeId,
                office.name(),
                props.radarStation(),
                props.timeZone(),
                office.sameAs()
        );
    }

    /**
     * Parses {@code "https://api.weather.gov/offices/LWX"} → {@code "LWX"}.
     * Returns null if the URL is null, blank, or doesn't end with an office id.
     */
    private static String parseOfficeId(String forecastOfficeUrl) {
        if (forecastOfficeUrl == null || forecastOfficeUrl.isBlank()) return null;
        int idx = forecastOfficeUrl.lastIndexOf(OFFICE_PATH);
        if (idx < 0) return null;
        String tail = forecastOfficeUrl.substring(idx + OFFICE_PATH.length());
        // Strip any trailing slashes or path segments
        int slash = tail.indexOf('/');
        return (slash >= 0 ? tail.substring(0, slash) : tail).trim();
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