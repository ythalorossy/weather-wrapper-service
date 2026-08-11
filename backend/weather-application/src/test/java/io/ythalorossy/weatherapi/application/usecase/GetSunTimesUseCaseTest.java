package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import io.ythalorossy.weatherapi.domain.port.SunTimesCache;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetSunTimesUseCaseTest {

    private SunTimesProvider provider;
    private SunTimesCache cache;
    private LocationResolver resolver;
    private LocationMetadataProvider metadataProvider;
    private GetSunTimesUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final SunTimes sample = new SunTimes(
            LocalDate.of(2026, 8, 9),
            Instant.parse("2026-08-09T10:42:00Z"),
            Instant.parse("2026-08-10T00:34:00Z"),
            "America/New_York");
    private final WeatherOffice office = new WeatherOffice(
            "LWX", "NWS Baltimore/Washington", "KLWX", "America/New_York", "https://api.weather.gov/offices/LWX");
    private static final Duration TTL = Duration.ofHours(48);

    @BeforeEach
    void setUp() {
        provider = mock(SunTimesProvider.class);
        cache = mock(SunTimesCache.class);
        resolver = mock(LocationResolver.class);
        metadataProvider = mock(LocationMetadataProvider.class);
        when(resolver.resolve("Arlington, VA")).thenReturn(arlington);
        when(metadataProvider.getOfficeFor(arlington)).thenReturn(Optional.of(office));
        useCase = new GetSunTimesUseCase(provider, cache, resolver, metadataProvider, TTL);
    }

    @Test
    void cacheHitReturnsCachedValue() {
        when(cache.get(any())).thenReturn(Optional.of(sample));
        Optional<SunTimes> result = useCase.execute("Arlington, VA");
        assertThat(result).contains(sample);
        verify(provider, never()).getSunTimes(any(), any(), any());
    }

    @Test
    void cacheMissCallsProviderAndWritesThrough() {
        when(cache.get(any())).thenReturn(Optional.empty());
        when(provider.getSunTimes(eq(arlington), any(), eq(ZoneId.of("America/New_York"))))
                .thenReturn(Optional.of(sample));
        Optional<SunTimes> result = useCase.execute("Arlington, VA");
        assertThat(result).contains(sample);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(cache).put(keyCap.capture(), eq(sample), eq(TTL));
        assertThat(keyCap.getValue()).startsWith("sun:38.8816,-77.0910:");
    }

    @Test
    void providerEmptyDoesNotCache() {
        when(cache.get(any())).thenReturn(Optional.empty());
        when(provider.getSunTimes(any(), any(), any())).thenReturn(Optional.empty());
        Optional<SunTimes> result = useCase.execute("Arlington, VA");
        assertThat(result).isEmpty();
        verify(cache, never()).put(any(), any(), any());
    }
}
