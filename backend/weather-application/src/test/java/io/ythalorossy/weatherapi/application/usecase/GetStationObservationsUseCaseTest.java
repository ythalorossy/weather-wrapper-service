package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.StationObservationProvider;
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

class GetStationObservationsUseCaseTest {

    private static final String STATION_ID = "KDCA";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StationObservation observation = new StationObservation(
            "KDCA",
            Instant.parse("2026-08-13T01:35:00Z"),
            Temperature.fahrenheit(78),
            71,
            "5 mph",
            "NW",
            "Raw METAR text",
            30.02
    );

    private StationObservationProvider observationProvider;
    private Cache<List<StationObservation>> observationsCache;
    private GetStationObservationsUseCase useCase;

    @BeforeEach
    void setUp() {
        observationProvider = mock(StationObservationProvider.class);
        observationsCache = mock(Cache.class);
        useCase = new GetStationObservationsUseCase(
                observationProvider, observationsCache, TTL);
    }

    @Test
    void cacheHitReturnsWithoutCallingProvider() {
        when(observationsCache.get(STATION_ID)).thenReturn(Optional.of(List.of(observation)));

        List<StationObservation> result = useCase.execute(STATION_ID);

        assertThat(result).containsExactly(observation);
        verify(observationProvider, never()).getObservations(any());
        verify(observationsCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromProviderAndWritesThroughWithConfiguredTtl() {
        when(observationsCache.get(STATION_ID)).thenReturn(Optional.empty());
        when(observationProvider.getObservations(STATION_ID)).thenReturn(List.of(observation));

        List<StationObservation> result = useCase.execute(STATION_ID);

        assertThat(result).containsExactly(observation);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(observationsCache).put(eq(STATION_ID), eq(List.of(observation)), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(TTL);
    }

    @Test
    void emptyListStillCaches() {
        when(observationsCache.get(STATION_ID)).thenReturn(Optional.empty());
        when(observationProvider.getObservations(STATION_ID)).thenReturn(List.of());

        List<StationObservation> result = useCase.execute(STATION_ID);

        assertThat(result).isEmpty();
        verify(observationsCache).put(eq(STATION_ID), eq(List.of()), any());
    }

    @Test
    void blankStationIdRejected() {
        assertThatThrownBy(() -> useCase.execute(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(observationProvider, never()).getObservations(any());
    }
}
