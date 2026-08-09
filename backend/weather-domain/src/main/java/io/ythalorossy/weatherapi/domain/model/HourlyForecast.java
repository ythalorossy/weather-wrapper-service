package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for an hourly forecast: a sequence of
 * {@link HourlyForecastPeriod}s for a single location.
 *
 * <p>Sibling to {@link WeatherForecast} (which uses 12-hour blocks); this one
 * carries finer-grained hourly data from the same NWS upstream endpoint.
 */
public record HourlyForecast(
        List<HourlyForecastPeriod> periods,
        Instant generatedAt,
        String source
) {

    public HourlyForecast {
        Objects.requireNonNull(periods, "periods must not be null");
        if (periods.isEmpty()) {
            throw new IllegalArgumentException("periods must not be empty");
        }
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        periods = List.copyOf(periods);
    }

    /** Convenience factory: {@code generatedAt} = {@link Instant#now()}. */
    public static HourlyForecast of(List<HourlyForecastPeriod> periods, String source) {
        return new HourlyForecast(periods, Instant.now(), source);
    }
}