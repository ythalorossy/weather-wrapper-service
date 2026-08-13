package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GetStationsUseCaseTest {

    @SuppressWarnings("unchecked")
    @Test
    void executesSuccessfully() {
        LocationResolver resolver = Mockito.mock(LocationResolver.class);
        StationsProvider provider = Mockito.mock(StationsProvider.class);
        Cache<List<Station>> cache = Mockito.mock(Cache.class);

        Location loc = new Location(38.88, -77.09, "Arlington, VA");
        when(resolver.resolve("Arlington, VA")).thenReturn(loc);
        when(cache.get(any())).thenReturn(Optional.empty());
        when(provider.getStations(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(new Station("KDCA", "National", 38.85, -77.04)));

        GetStationsUseCase uc = new GetStationsUseCase(resolver, provider, cache);
        List<Station> stations = uc.execute("Arlington, VA");

        assertThat(stations).hasSize(1);
        assertThat(stations.get(0).stationId()).isEqualTo("KDCA");
    }
}
