package io.ythalorossy.weatherapi.infrastructure.weather;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.HourlyForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.GridpointHourlyForecastResponse;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * NWS adapter for {@link HourlyWeatherProvider}.
 *
 * <p>Two-step flow identical to {@link NwsWeatherProvider}: {@code /points}
 * → gridpoint triple, then {@code /gridpoints/.../forecast/hourly} → 156
 * hours of forecast periods.
 */
@Component
public class NwsHourlyWeatherProvider implements HourlyWeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsHourlyWeatherProvider.class);
    private static final String SOURCE = "National Weather Service (api.weather.gov)";
    private static final String TIMER_NAME = "weather.provider.nws";

    private final RestClient client;
    private final NwsPointsService pointsService;
    private final MeterRegistry meterRegistry;

    public NwsHourlyWeatherProvider(RestClient nwsRestClient,
                                    NwsPointsService pointsService,
                                    MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.pointsService = pointsService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<HourlyForecast> getHourlyForecast(Location location) {
        Objects.requireNonNull(location, "location");
        log.debug("Fetching hourly forecast for {} ({},{})", location.displayName(),
                location.latitude(), location.longitude());

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            HourlyForecast forecast = doFetch(location);
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "hourly", "outcome", "success"));
            return Optional.of(forecast);
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(TIMER_NAME, "endpoint", "hourly", "outcome", "failure"));
            throw e;
        }
    }

    private HourlyForecast doFetch(Location location) {
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

        // Step 2: hourly forecast from the gridpoint
        GridpointHourlyForecastResponse forecast;
        try {
            forecast = client.get()
                    .uri("/gridpoints/{gridId}/{x},{y}/forecast/hourly",
                            props.gridId(), props.gridX(), props.gridY())
                    .retrieve()
                    .body(GridpointHourlyForecastResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new WeatherProviderUnavailableException(
                    "NWS /forecast/hourly returned " + e.getStatusCode() + " for " + location.displayName(), e);
        } catch (Exception e) {
            throw new WeatherProviderUnavailableException(
                    "Failed to call NWS /forecast/hourly for " + location.displayName() + ": " + e.getMessage(), e);
        }

        if (forecast == null || forecast.properties() == null
                || forecast.properties().periods() == null
                || forecast.properties().periods().isEmpty()) {
            throw new WeatherProviderUnavailableException(
                    "NWS /forecast/hourly returned empty body for " + location.displayName());
        }

        List<HourlyForecastPeriod> periods = forecast.properties().periods().stream()
                .map(NwsHourlyWeatherProvider::toDomainPeriod)
                .toList();

        return HourlyForecast.of(periods, SOURCE);
    }

    private static HourlyForecastPeriod toDomainPeriod(GridpointHourlyForecastResponse.Period p) {
        Temperature temp = "C".equalsIgnoreCase(p.temperatureUnit())
                ? Temperature.celsius(p.temperature())
                : Temperature.fahrenheit(p.temperature());
        Instant startTime;
        try {
            startTime = Instant.parse(p.startTime());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new WeatherProviderUnavailableException(
                    "NWS /forecast/hourly returned unparseable startTime: " + p.startTime(), e);
        }
        return new HourlyForecastPeriod(
                startTime,
                temp,
                p.windSpeed(),
                p.windDirection(),
                p.shortForecast(),
                p.isDaytime()
        );
    }
}