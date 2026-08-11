package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetAfdUseCaseTest {

    private AreaForecastDiscussionProvider provider;
    private Cache<AfdProduct> cache;
    private LocationResolver locationResolver;
    private LocationMetadataProvider metadataProvider;
    private GetAfdUseCase useCase;

    private final Location location = new Location(38.8816, -77.0910, "Arlington, VA");
    private final WeatherOffice office = new WeatherOffice(
            "LWX", "NWS Baltimore/Washington", "KLWX", "America/New_York", "https://www.weather.gov/lwx");

    @BeforeEach
    void setUp() {
        provider = mock(AreaForecastDiscussionProvider.class);
        cache = mock(Cache.class);
        locationResolver = mock(LocationResolver.class);
        metadataProvider = mock(LocationMetadataProvider.class);
        useCase = new GetAfdUseCase(provider, cache, locationResolver, metadataProvider, Duration.ofMinutes(30));
        when(locationResolver.resolve("Arlington, VA")).thenReturn(location);
    }

    @Test
    void returnsCachedProductOnHitWithoutCallingProvider() {
        AfdProduct cached = new AfdProduct("LWX", Instant.parse("2026-08-10T14:35:00Z"), "body");
        when(metadataProvider.getOfficeFor(location)).thenReturn(Optional.of(office));
        when(cache.get("LWX")).thenReturn(Optional.of(cached));

        Optional<AfdProduct> result = useCase.execute("Arlington, VA");

        assertThat(result).contains(cached);
        verify(provider, never()).getLatest(any());
    }

    @Test
    void fetchesAndCachesOnMiss() {
        when(metadataProvider.getOfficeFor(location)).thenReturn(Optional.of(office));
        when(cache.get("LWX")).thenReturn(Optional.empty());
        AfdProduct fresh = new AfdProduct("LWX", Instant.parse("2026-08-10T14:35:00Z"), "fresh body");
        when(provider.getLatest("LWX")).thenReturn(Optional.of(fresh));

        Optional<AfdProduct> result = useCase.execute("Arlington, VA");

        assertThat(result).contains(fresh);
        verify(cache).put("LWX", fresh, Duration.ofMinutes(30));
    }

    @Test
    void returnsEmptyAndDoesNotCacheWhenProviderReturnsEmpty() {
        when(metadataProvider.getOfficeFor(location)).thenReturn(Optional.of(office));
        when(cache.get("LWX")).thenReturn(Optional.empty());
        when(provider.getLatest("LWX")).thenReturn(Optional.empty());

        Optional<AfdProduct> result = useCase.execute("Arlington, VA");

        assertThat(result).isEmpty();
        verify(cache, never()).put(any(), any(), any());
    }

    @Test
    void returnsEmptyWhenOfficeIsMissing() {
        when(metadataProvider.getOfficeFor(location)).thenReturn(Optional.empty());

        Optional<AfdProduct> result = useCase.execute("Arlington, VA");

        assertThat(result).isEmpty();
        verify(provider, never()).getLatest(any());
        verify(cache, never()).get(any());
    }

    @Test
    void bubblesUpLocationNotFound() {
        when(locationResolver.resolve("Nowhere")).thenThrow(new LocationNotFoundException("Nowhere"));

        assertThatThrownBy(() -> useCase.execute("Nowhere"))
                .isInstanceOf(LocationNotFoundException.class);
    }
}
