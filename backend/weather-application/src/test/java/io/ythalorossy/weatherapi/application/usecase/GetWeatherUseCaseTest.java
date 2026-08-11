package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    private WeatherProvider weather;
    private WeatherCache weatherCache;
    private LocationResolver locationResolver;
    private GetWeatherUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final WeatherForecast forecast = new WeatherForecast(
            List.of(new ForecastPeriod("Today", Temperature.fahrenheit(85), "5 mph", "NW",
                    "Sunny", "Sunny, with a high near 85.", true)),
            Instant.now(),
            "NWS"
    );
    private static final String CITY = "Arlington, VA";
    private static final String WEATHER_KEY = "weather:38.8816,-77.0910";

    @BeforeEach
    void setUp() {
        weather = mock(WeatherProvider.class);
        weatherCache = mock(WeatherCache.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve(CITY)).thenReturn(arlington);
        useCase = new GetWeatherUseCase(
                weather, weatherCache, locationResolver, Duration.ofHours(12));
    }

    // -- Weather cache hit

    @Test
    void weatherCacheHitReturnsWithoutCallingProvider() {
        when(weatherCache.get(WEATHER_KEY))
                .thenReturn(java.util.Optional.of(forecast));

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.location()).isEqualTo(arlington);
        assertThat(result.forecast()).isEqualTo(forecast);
        verify(weather, never()).getForecast(any());
        verify(weatherCache, never()).put(anyString(), any(), any());
    }

    // -- Weather cache miss

    @Test
    void weatherCacheMissCallsProviderAndWritesThroughWith12HourTtl() {
        when(weatherCache.get(WEATHER_KEY)).thenReturn(java.util.Optional.empty());
        when(weather.getForecast(arlington)).thenReturn(forecast);

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.forecast()).isEqualTo(forecast);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(weatherCache).put(eq(WEATHER_KEY), eq(forecast), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(12));
    }

    // -- Resolver throws LocationNotFound

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(weather, never()).getForecast(any());
        verify(weatherCache, never()).put(anyString(), any(), any());
    }

    // -- Constructor validation

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