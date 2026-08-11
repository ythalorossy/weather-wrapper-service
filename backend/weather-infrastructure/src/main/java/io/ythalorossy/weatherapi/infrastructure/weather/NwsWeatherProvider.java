package io.ythalorossy.weatherapi.infrastructure.weather;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.GridpointForecastResponse;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * NWS (api.weather.gov) adapter for {@link WeatherProvider}.
 *
 * <p>Two-step flow: {@code /points/{lat},{lon}} → gridpoint triple,
 * then {@code /gridpoints/{gridId}/{x},{y}/forecast} → forecast periods.
 *
 * <p>Exposes a Micrometer timer under {@code weather.provider.nws} with tag
 * {@code outcome=success|failure}, capturing the wall-clock duration of the
 * entire two-step call (both NWS requests in series).
 */
@Component
public class NwsWeatherProvider implements WeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsWeatherProvider.class);
    private static final String SOURCE = "National Weather Service (api.weather.gov)";
    private static final String TIMER_NAME = "weather.provider.nws";

    private final RestClient client;
    private final NwsPointsService pointsService;
    private final MeterRegistry meterRegistry;

    public NwsWeatherProvider(RestClient nwsRestClient,
                              NwsPointsService pointsService,
                              MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.pointsService = pointsService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public WeatherForecast getForecast(Location location) {
        Objects.requireNonNull(location, "location");
        log.debug("Fetching forecast for {} ({},{})", location.displayName(),
                location.latitude(), location.longitude());

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            WeatherForecast forecast = doFetch(location);
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "success"));
            return forecast;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "failure"));
            throw e;
        }
    }

    private WeatherForecast doFetch(Location location) {
        // Step 1: lat/lon → gridpoint (cached per coordinate)
        PointsResponse points = pointsService.lookup(location.latitude(), location.longitude());

        if (points == null || points.properties() == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned empty body for " + location.displayName());
        }

        var props = points.properties();
        if (props.gridId() == null || props.gridId().isBlank()) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned no gridId for " + location.displayName());
        }

        // Step 2: forecast from the gridpoint
        GridpointForecastResponse forecast = NwsClient.invoke(
                () -> client.get()
                        .uri("/gridpoints/{gridId}/{x},{y}/forecast",
                                props.gridId(), props.gridX(), props.gridY())
                        .retrieve()
                        .body(GridpointForecastResponse.class),
                "NWS /forecast",
                location
        );

        if (forecast == null || forecast.properties() == null
                || forecast.properties().periods() == null
                || forecast.properties().periods().isEmpty()) {
            throw new WeatherProviderUnavailableException(
                    "NWS /forecast returned empty body for " + location.displayName());
        }

        List<ForecastPeriod> periods = forecast.properties().periods().stream()
                .map(NwsWeatherProvider::toDomainPeriod)
                .toList();

        return new WeatherForecast(periods, Instant.now(), SOURCE);
    }

    private static ForecastPeriod toDomainPeriod(GridpointForecastResponse.Period p) {
        Temperature temp = "C".equalsIgnoreCase(p.temperatureUnit())
                ? Temperature.celsius(p.temperature())
                : Temperature.fahrenheit(p.temperature());
        return new ForecastPeriod(
                p.name(),
                temp,
                p.windSpeed(),
                p.windDirection(),
                p.shortForecast(),
                p.detailedForecast(),
                p.isDaytime()
        );
    }
}