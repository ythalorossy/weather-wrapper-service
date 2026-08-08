package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.LocationCache;
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
    private WeatherCache weatherCache;
    private LocationCache locationCache;
    private GetWeatherUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final WeatherForecast forecast = new WeatherForecast(
            List.of(new ForecastPeriod("Today", Temperature.fahrenheit(85), "5 mph", "NW",
                    "Sunny", "Sunny, with a high near 85.", true)),
            Instant.now(),
            "NWS"
    );
    private static final String CITY = "Arlington, VA";
    private static final String GEO_KEY = "geo:arlington, va";
    private static final String WEATHER_KEY = "weather:38.88,-77.09";

    @BeforeEach
    void setUp() {
        geocoding = mock(GeocodingProvider.class);
        weather = mock(WeatherProvider.class);
        weatherCache = mock(WeatherCache.class);
        locationCache = mock(LocationCache.class);
        useCase = new GetWeatherUseCase(
                geocoding, weather, weatherCache, locationCache,
                Duration.ofHours(12), Duration.ofDays(30), Duration.ofSeconds(60));
    }

    // -- Happy path: cache hit on both layers

    @Test
    void fullCacheHitReturnsWithoutCallingUpstream() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.of(arlington));
        when(weatherCache.get(WEATHER_KEY)).thenReturn(Optional.of(forecast));

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.location()).isEqualTo(arlington);
        assertThat(result.forecast()).isEqualTo(forecast);
        verify(geocoding, never()).findLocation(anyString());
        verify(weather, never()).getForecast(any());
        verify(locationCache, never()).put(anyString(), any(), any());
        verify(weatherCache, never()).put(anyString(), any(), any());
    }

    // -- Layer 1: geocoding cache hit, weather cache miss

    @Test
    void locationCacheHitSkipsGeocoderAndStillFetchesForecastOnWeatherMiss() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.of(arlington));
        when(weatherCache.get(WEATHER_KEY)).thenReturn(Optional.empty());
        when(weather.getForecast(arlington)).thenReturn(forecast);

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.forecast()).isEqualTo(forecast);
        verify(geocoding, never()).findLocation(anyString());

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(weatherCache).put(eq(WEATHER_KEY), eq(forecast), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(12));
        verify(locationCache, never()).put(anyString(), any(), any());
    }

    // -- Layer 1: geocoding cache miss, geocoder hit, weather cache hit

    @Test
    void geocodingCacheMissCallsGeocoderAndWritesThrough() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.empty());
        when(geocoding.findLocation(CITY)).thenReturn(Optional.of(arlington));
        when(weatherCache.get(WEATHER_KEY)).thenReturn(Optional.of(forecast));

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.location()).isEqualTo(arlington);
        assertThat(result.forecast()).isEqualTo(forecast);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(locationCache).put(eq(GEO_KEY), eq(arlington), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(30));
        verify(geocoding, never()).findLocation("different-city");  // never called twice
    }

    // -- Both layers miss: full upstream call

    @Test
    void bothCachesMissCallsBothUpstreamsAndWritesThroughBoth() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.empty());
        when(geocoding.findLocation(CITY)).thenReturn(Optional.of(arlington));
        when(weatherCache.get(WEATHER_KEY)).thenReturn(Optional.empty());
        when(weather.getForecast(arlington)).thenReturn(forecast);

        WeatherQueryResult result = useCase.execute(CITY);

        assertThat(result.forecast()).isEqualTo(forecast);

        ArgumentCaptor<Duration> locationTtl = ArgumentCaptor.forClass(Duration.class);
        verify(locationCache).put(eq(GEO_KEY), eq(arlington), locationTtl.capture());
        assertThat(locationTtl.getValue()).isEqualTo(Duration.ofDays(30));

        ArgumentCaptor<Duration> weatherTtl = ArgumentCaptor.forClass(Duration.class);
        verify(weatherCache).put(eq(WEATHER_KEY), eq(forecast), weatherTtl.capture());
        assertThat(weatherTtl.getValue()).isEqualTo(Duration.ofHours(12));
    }

    // -- City not found (negative path)

    @Test
    void unknownCityThrowsLocationNotFoundAndMarksAbsentCache() {
        when(locationCache.get("geo:nowhereville")).thenReturn(Optional.empty());
        when(locationCache.isAbsent("geo:nowhereville")).thenReturn(false);
        when(geocoding.findLocation("NowhereVille")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        ArgumentCaptor<Duration> absentTtl = ArgumentCaptor.forClass(Duration.class);
        verify(locationCache).markAbsent(eq("geo:nowhereville"), absentTtl.capture());
        assertThat(absentTtl.getValue()).isEqualTo(Duration.ofSeconds(60));
        verify(weather, never()).getForecast(any());
        verify(weatherCache, never()).put(anyString(), any(), any());
        verify(locationCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cachedAbsentSkipsGeocoder() {
        when(locationCache.get("geo:nowhereville")).thenReturn(Optional.empty());
        when(locationCache.isAbsent("geo:nowhereville")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(geocoding, never()).findLocation(anyString());
        verify(weather, never()).getForecast(any());
        verify(locationCache, never()).put(anyString(), any(), any());
        verify(locationCache, never()).markAbsent(anyString(), any());
    }

    // -- Input validation

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

    // -- Constructor validation

    @Test
    void constructorRejectsNullCollaborators() {
        assertThatThrownBy(() -> new GetWeatherUseCase(null, weather, weatherCache, locationCache,
                Duration.ofHours(1), Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, null, weatherCache, locationCache,
                Duration.ofHours(1), Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, null, locationCache,
                Duration.ofHours(1), Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, null,
                Duration.ofHours(1), Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                null, Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ofHours(1), null, Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ofHours(1), Duration.ofDays(1), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNonPositiveWeatherTtl() {
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ZERO, Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weatherCacheTtl");
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ofSeconds(-1), Duration.ofDays(1), Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNonPositiveLocationTtl() {
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ofHours(1), Duration.ZERO, Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locationCacheTtl");
    }

    @Test
    void constructorRejectsNonPositiveAbsentTtl() {
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ofHours(1), Duration.ofDays(1), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locationAbsentTtl");
        assertThatThrownBy(() -> new GetWeatherUseCase(geocoding, weather, weatherCache, locationCache,
                Duration.ofHours(1), Duration.ofDays(1), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}