package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.LocationCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
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

class LocationResolverTest {

    private GeocodingProvider geocoding;
    private LocationCache locationCache;
    private LocationResolver resolver;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private static final String CITY = "Arlington, VA";
    private static final String GEO_KEY = "geo:arlington, va";

    @BeforeEach
    void setUp() {
        geocoding = mock(GeocodingProvider.class);
        locationCache = mock(LocationCache.class);
        resolver = new LocationResolver(
                geocoding, locationCache, Duration.ofDays(30), Duration.ofSeconds(60));
    }

    @Test
    void positiveCacheHitReturnsWithoutCallingGeocoder() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.of(arlington));

        Location result = resolver.resolve(CITY);

        assertThat(result).isEqualTo(arlington);
        verify(geocoding, never()).findLocation(anyString());
        verify(locationCache, never()).put(anyString(), any(), any());
    }

    @Test
    void negativeCacheHitThrowsWithoutCallingGeocoder() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.empty());
        when(locationCache.isAbsent(GEO_KEY)).thenReturn(true);

        assertThatThrownBy(() -> resolver.resolve(CITY))
                .isInstanceOf(LocationNotFoundException.class);

        verify(geocoding, never()).findLocation(anyString());
    }

    @Test
    void cacheMissCallsGeocoderAndWritesThroughWithLongTtl() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.empty());
        when(locationCache.isAbsent(GEO_KEY)).thenReturn(false);
        when(geocoding.findLocation(CITY)).thenReturn(Optional.of(arlington));

        Location result = resolver.resolve(CITY);

        assertThat(result).isEqualTo(arlington);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(locationCache).put(eq(GEO_KEY), eq(arlington), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void geocoderMissMarksAbsentCacheWithShortTtlAndThrows() {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.empty());
        when(locationCache.isAbsent(GEO_KEY)).thenReturn(false);
        when(geocoding.findLocation(CITY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(CITY))
                .isInstanceOf(LocationNotFoundException.class);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(locationCache).markAbsent(eq(GEO_KEY), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(60));
        verify(locationCache, never()).put(anyString(), any(), any());
    }

    @Test
    void blankCityRejectedBeforeAnyCacheLookup() {
        assertThatThrownBy(() -> resolver.resolve("   "))
                .isInstanceOf(IllegalArgumentException.class);

        verify(locationCache, never()).get(anyString());
        verify(locationCache, never()).isAbsent(anyString());
        verify(geocoding, never()).findLocation(anyString());
    }
}