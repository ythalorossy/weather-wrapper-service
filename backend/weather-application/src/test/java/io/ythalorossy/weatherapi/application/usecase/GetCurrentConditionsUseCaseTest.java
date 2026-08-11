package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
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

class GetCurrentConditionsUseCaseTest {

    private static final String CITY = "Arlington, VA";
    private static final Location ARLINGTON = new Location(38.8816, -77.0910, "Arlington, VA");
    private static final String OBS_KEY = "38.8816,-77.0910";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final Observation obs = new Observation(
            "KIAD",
            "Washington/Dulles International Airport, VA",
            Instant.parse("2026-08-08T21:35:00Z"),
            78.4, 71.6, 5.2, 315, "NW", 83.5, 30.02, "Cloudy"
    );

    private ObservationProvider observationProvider;
    private Cache<Observation> observationCache;
    private LocationResolver locationResolver;
    private GetCurrentConditionsUseCase useCase;

    @BeforeEach
    void setUp() {
        observationProvider = mock(ObservationProvider.class);
        observationCache = mock(Cache.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve(CITY)).thenReturn(ARLINGTON);
        useCase = new GetCurrentConditionsUseCase(observationProvider, observationCache, locationResolver, TTL);
    }

    @Test
    void cacheHitReturnsWithoutCallingProvider() {
        when(observationCache.get(OBS_KEY)).thenReturn(Optional.of(obs));

        Observation result = useCase.execute(CITY);

        assertThat(result).isEqualTo(obs);
        verify(observationProvider, never()).getCurrentObservation(any());
        verify(observationCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromProviderAndWritesThroughWith10MinTtl() {
        when(observationCache.get(OBS_KEY)).thenReturn(Optional.empty());
        when(observationProvider.getCurrentObservation(ARLINGTON)).thenReturn(Optional.of(obs));

        Observation result = useCase.execute(CITY);

        assertThat(result).isEqualTo(obs);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(observationCache).put(eq(OBS_KEY), eq(obs), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(TTL);
    }

    @Test
    void emptyResultFromProviderThrows() {
        when(observationCache.get(OBS_KEY)).thenReturn(Optional.empty());
        when(observationProvider.getCurrentObservation(ARLINGTON)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(CITY))
                .isInstanceOf(IllegalStateException.class);
        verify(observationCache, never()).put(anyString(), any(), any());
    }

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(observationProvider, never()).getCurrentObservation(any());
    }
}
