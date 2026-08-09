package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.HourlyForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.port.HourlyForecastCache;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetHourlyForecastUseCaseTest {

    private HourlyWeatherProvider hourlyProvider;
    private HourlyForecastCache hourlyCache;
    private LocationResolver locationResolver;
    private GetHourlyForecastUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final HourlyForecast hourly = new HourlyForecast(
            List.of(new HourlyForecastPeriod(
                    Instant.parse("2026-08-08T19:00:00Z"),
                    Temperature.fahrenheit(85),
                    "5 mph", "NW", "Sunny", true)),
            Instant.now(),
            "NWS"
    );
    private static final String CITY = "Arlington, VA";
    private static final String HOURLY_KEY = "hourly:38.88,-77.09";

    @BeforeEach
    void setUp() {
        hourlyProvider = mock(HourlyWeatherProvider.class);
        hourlyCache = mock(HourlyForecastCache.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve(CITY)).thenReturn(arlington);
        useCase = new GetHourlyForecastUseCase(
                hourlyProvider, hourlyCache, locationResolver, Duration.ofHours(12));
    }

    @Test
    void cacheHitReturnsWithoutCallingProvider() {
        when(hourlyCache.get(HOURLY_KEY)).thenReturn(Optional.of(hourly));

        HourlyForecast result = useCase.execute(CITY);

        assertThat(result).isEqualTo(hourly);
        verify(hourlyProvider, never()).getHourlyForecast(any());
        verify(hourlyCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromProviderAndWritesThroughWith12HourTtl() {
        when(hourlyCache.get(HOURLY_KEY)).thenReturn(Optional.empty());
        when(hourlyProvider.getHourlyForecast(arlington)).thenReturn(Optional.of(hourly));

        HourlyForecast result = useCase.execute(CITY);

        assertThat(result).isEqualTo(hourly);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(hourlyCache).put(eq(HOURLY_KEY), eq(hourly), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException.class);

        verify(hourlyProvider, never()).getHourlyForecast(any());
        verify(hourlyCache, never()).put(anyString(), any(), any());
    }
}