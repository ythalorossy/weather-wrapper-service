package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;
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

class GetActiveAlertsUseCaseTest {

    private static final String CITY = "Arlington, VA";
    private static final Location ARLINGTON = new Location(38.8816, -77.0910, "Arlington, VA");
    private static final String ALERTS_KEY = "38.8816,-77.0910";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final WeatherAlert alert = new WeatherAlert(
            "urn:oid:2.49.0.1.test",
            "Severe Thunderstorm Warning",
            AlertSeverity.Severe,
            AlertCertainty.Likely,
            AlertUrgency.Expected,
            AlertCategory.Met,
            "Severe Thunderstorm Warning issued ...",
            "Long description...",
            "Take shelter.",
            "Arlington County",
            Instant.now(),
            Instant.now(),
            Instant.now().plusSeconds(3600),
            "https://example.com"
    );

    private AlertProvider alertProvider;
    private Cache<AlertsPayload> alertCache;
    private LocationResolver locationResolver;
    private GetActiveAlertsUseCase useCase;

    @BeforeEach
    void setUp() {
        alertProvider = mock(AlertProvider.class);
        alertCache = mock(Cache.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve(CITY)).thenReturn(ARLINGTON);
        useCase = new GetActiveAlertsUseCase(alertProvider, alertCache, locationResolver, TTL);
    }

    @Test
    void cacheHitReturnsWithoutCallingProvider() {
        AlertsPayload cached = new AlertsPayload(List.of(alert));
        when(alertCache.get(ALERTS_KEY)).thenReturn(Optional.of(cached));

        List<WeatherAlert> result = useCase.execute(CITY);

        assertThat(result).containsExactly(alert);
        verify(alertProvider, never()).getActiveAlerts(any());
        verify(alertCache, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromProviderAndWritesThroughWith5MinTtl() {
        when(alertCache.get(ALERTS_KEY)).thenReturn(Optional.empty());
        when(alertProvider.getActiveAlerts(ARLINGTON)).thenReturn(List.of(alert));

        List<WeatherAlert> result = useCase.execute(CITY);

        assertThat(result).containsExactly(alert);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(alertCache).put(eq(ALERTS_KEY), eq(new AlertsPayload(List.of(alert))), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(TTL);
    }

    @Test
    void emptyListStillCaches() {
        when(alertCache.get(ALERTS_KEY)).thenReturn(Optional.empty());
        when(alertProvider.getActiveAlerts(ARLINGTON)).thenReturn(List.of());

        List<WeatherAlert> result = useCase.execute(CITY);

        assertThat(result).isEmpty();
        verify(alertCache).put(eq(ALERTS_KEY), eq(new AlertsPayload(List.of())), any());
    }

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(alertProvider, never()).getActiveAlerts(any());
    }
}
