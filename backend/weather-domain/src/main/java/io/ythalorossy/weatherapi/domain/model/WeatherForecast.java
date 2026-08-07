package io.ythalorossy.weatherapi.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a weather forecast: a sequence of {@link ForecastPeriod}s
 * for a single location at a single point in time.
 *
 * <p>Immutable. Source identifies which upstream provider produced this forecast
 * (e.g., "National Weather Service") so consumers can audit provenance.
 */
public record WeatherForecast(
        List<ForecastPeriod> periods,
        Instant generatedAt,
        String source
) {

    public WeatherForecast {
        Objects.requireNonNull(periods, "periods must not be null");
        if (periods.isEmpty()) {
            throw new IllegalArgumentException("periods must not be empty");
        }
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        periods = List.copyOf(periods); // defensive immutable copy
    }

    /**
     * Convenience factory. {@code generatedAt} is set to {@link Instant#now()}.
     */
    public static WeatherForecast of(List<ForecastPeriod> periods, String source) {
        return new WeatherForecast(periods, Instant.now(), source);
    }
}