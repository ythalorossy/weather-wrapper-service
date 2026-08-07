package io.ythalorossy.weatherapi.infrastructure.weather;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * NWS (api.weather.gov) adapter for {@link WeatherProvider}.
 *
 * <p>Two-step flow: {@code /points/{lat},{lon}} → gridpoint triple,
 * then {@code /gridpoints/{gridId}/{x},{y}/forecast} → forecast periods.
 */
@Component
public class NwsWeatherProvider implements WeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsWeatherProvider.class);
    private static final String SOURCE = "National Weather Service (api.weather.gov)";

    private final RestClient client;

    public NwsWeatherProvider(RestClient nwsRestClient) {
        this.client = nwsRestClient;
    }

    @Override
    public WeatherForecast getForecast(Location location) {
        Objects.requireNonNull(location, "location");
        log.debug("Fetching forecast for {} ({},{})", location.displayName(),
                location.latitude(), location.longitude());

        // Step 1: lat/lon → gridpoint
        PointsResponse points = invoke(
                () -> client.get()
                        .uri("/points/{lat},{lon}", location.latitude(), location.longitude())
                        .retrieve()
                        .body(PointsResponse.class),
                "NWS /points",
                location
        );

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
        GridpointForecastResponse forecast = invoke(
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

    /**
     * Invokes the supplied HTTP call and translates client/server errors into
     * {@link WeatherProviderUnavailableException}. The supplier pattern lets us
     * catch exceptions from the actual {@code .retrieve().body()} chain (which
     * throws before the body is returned).
     */
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