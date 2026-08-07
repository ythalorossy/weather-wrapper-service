package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
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

    private GeocodingProvider geocoding;
    private WeatherProvider weather;
    private WeatherCache cache;
    private GetWeatherUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final WeatherForecast forecast = new WeatherForecast(
            List.of(new ForecastPeriod("Today", Temperature.fahrenheit(85), "5 mph", "NW",
                    "Sunny", "Sunny, with a high near 85.", true)),
            Instant.now(),
            "NWS"
    );
    private static final String CITY = "Arlington, VA";
    private static final String CACHE_KEY = "weather:38.88,-77.09";

    @BeforeEach
    void setUp() {
        geocoding = mock(GeocodingProvider.class);
        weather = mock(WeatherProvider.class);
        cache = mock(WeatherCache.class);
        useCase = new GetWeatherUseCase(geocoding, weather, cache, Duration.ofHours(12));
    }

    @Test
    void cacheHitReturnsCachedForecastWithoutCallingUpstream() {
        when(geocoding.findLocation(CITY)).thenReturn(Optional.of(arlington));
        when(cache.get(CACHE_KEY)).thenReturn(Optional.of(forecast));

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.location()).isEqualTo(arlington);
        assertThat(result.forecast()).isEqualTo(forecast);
        verify(weather, never()).getForecast(any());
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissCallsUpstreamAndWritesThrough() {
        when(geocoding.findLocation(CITY)).thenReturn(Optional.of(arlington));
        when(cache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(weather.getForecast(arlington)).thenReturn(forecast);

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.location()).isEqualTo(arlington);
        assertThat(result.forecast()).isEqualTo(forecast);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(cache).put(eq(CACHE_KEY), eq(forecast), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void unknownCityThrowsLocationNotFound() {
        when(geocoding.findLocation("NowhereVille")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);
        verify(weather, never()).getForecast(any());
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void blankCityRejected() {
        assertThatThrownBy(() -> useCase.execute(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullCityRejected() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNullCollaborators() {
        assertThatThrownBy(() -> new GetWeatherUseCase(null, weather, cache, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, null, cache, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, null, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, cache, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNonPositiveTtl() {
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, cache, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, cache, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}