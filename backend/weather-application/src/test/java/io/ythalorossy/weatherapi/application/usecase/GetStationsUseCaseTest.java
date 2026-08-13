package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Gridpoint;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.PointsProvider;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class GetStationsUseCaseTest {

    private static final String CITY = "Arlington, VA";
    private static final Location ARLINGTON = new Location(38.8816, -77.0910, "Arlington, VA");
    private static final String STATIONS_KEY = "38.8816,-77.0910";
    private static final Gridpoint GRIDPOINT = new Gridpoint("LWX", 76, 103);

    private final Station station = new Station("KDCA", "National", 38.8518, -77.0403);

    private LocationResolver locationResolver;
    private StationsProvider stationsProvider;
    private PointsProvider pointsProvider;
    private Cache<List<Station>> stationsCache;
    private GetStationsUseCase useCase;

    @BeforeEach
    void setUp() {
        locationResolver = mock(LocationResolver.class);
        stationsProvider = mock(StationsProvider.class);
        pointsProvider = mock(PointsProvider.class);
        stationsCache = mock(Cache.class);
        useCase = new GetStationsUseCase(
                locationResolver, stationsProvider, pointsProvider, stationsCache);
    }

    @Test
    void cacheHitReturnsWithoutCallingProviders() {
        when(locationResolver.resolve(CITY)).thenReturn(ARLINGTON);
        when(stationsCache.get(STATIONS_KEY)).thenReturn(Optional.of(List.of(station)));

        List<Station> result = useCase.execute(CITY);

        assertThat(result).containsExactly(station);
        verify(pointsProvider, never()).getGridpoint(any());
        verify(stationsProvider, never()).getStations(any(), any(Integer.class), any(Integer.class));
        verify(stationsCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissResolvesGridpointAndFetchesStations() {
        when(stationsCache.get(STATIONS_KEY)).thenReturn(Optional.empty());
        when(locationResolver.resolve(CITY)).thenReturn(ARLINGTON);
        when(pointsProvider.getGridpoint(ARLINGTON)).thenReturn(GRIDPOINT);
        when(stationsProvider.getStations("LWX", 76, 103)).thenReturn(List.of(station));

        List<Station> result = useCase.execute(CITY);

        assertThat(result).containsExactly(station);
        verify(pointsProvider).getGridpoint(ARLINGTON);
        verify(stationsProvider).getStations("LWX", 76, 103);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(stationsCache).put(keyCaptor.capture(), eq(List.of(station)), any());
        assertThat(keyCaptor.getValue()).isEqualTo(STATIONS_KEY);
    }

    @Test
    void gridpointResolutionFailurePropagates() {
        when(stationsCache.get(STATIONS_KEY)).thenReturn(Optional.empty());
        when(locationResolver.resolve(CITY)).thenReturn(ARLINGTON);
        when(pointsProvider.getGridpoint(ARLINGTON))
                .thenThrow(new IllegalStateException("NWS /points unreachable"));

        assertThatThrownBy(() -> useCase.execute(CITY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NWS /points unreachable");
        verify(stationsProvider, never())
                .getStations(any(), any(Integer.class), any(Integer.class));
        verify(stationsCache, never()).put(anyString(), any(), any());
    }

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);
        verify(pointsProvider, never()).getGridpoint(any());
        verify(stationsProvider, never())
                .getStations(any(), any(Integer.class), any(Integer.class));
    }
}
