package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
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

class GetWeatherUseCaseTest {

    private static final String CITY = "Arlington, VA";
    private static final Location ARLINGTON = new Location(38.8816, -77.0910, "Arlington, VA");
    private static final String WEATHER_KEY = "38.8816,-77.0910";
    private static final Duration TTL = Duration.ofHours(12);

    private final WeatherForecast forecast = new WeatherForecast(
            List.of(new ForecastPeriod("Today", Temperature.fahrenheit(85), "5 mph", "NW",
                    "Sunny", "Sunny, with a high near 85.", true)),
            Instant.now(),
            "NWS"
    );

    private WeatherProvider weather;
    private Cache<WeatherForecast> weatherCache;
    private LocationResolver locationResolver;
    private GetWeatherUseCase useCase;

    @BeforeEach
    void setUp() {
        weather = mock(WeatherProvider.class);
        weatherCache = mock(Cache.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve(CITY)).thenReturn(ARLINGTON);
        useCase = new GetWeatherUseCase(weather, weatherCache, locationResolver, TTL);
    }

    @Test
    void cacheHitReturnsWithoutCallingProvider() {
        when(weatherCache.get(WEATHER_KEY)).thenReturn(Optional.of(forecast));

        Location location = useCase.execute(CITY);

        assertThat(location).isEqualTo(ARLINGTON);
        verify(weather, never()).getForecast(any());
        verify(weatherCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissCallsProviderAndWritesThroughWith12HourTtl() {
        when(weatherCache.get(WEATHER_KEY)).thenReturn(Optional.empty());
        when(weather.getForecast(ARLINGTON)).thenReturn(forecast);

        Location location = useCase.execute(CITY);

        assertThat(location).isEqualTo(ARLINGTON);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(weatherCache).put(eq(WEATHER_KEY), eq(forecast), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(TTL);
    }

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(weather, never()).getForecast(any());
        verify(weatherCache, never()).put(anyString(), any(), any());
    }

    @Test
    void constructorRejectsNullCollaborators() {
        assertThatThrownBy(() -> new GetWeatherUseCase(
                null, weatherCache, locationResolver, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(
                weather, null, locationResolver, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(
                weather, weatherCache, null, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNonPositiveWeatherTtl() {
        assertThatThrownBy(() -> new GetWeatherUseCase(
                weather, weatherCache, locationResolver, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weatherCacheTtl");
        assertThatThrownBy(() -> new GetWeatherUseCase(
                weather, weatherCache, locationResolver, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
