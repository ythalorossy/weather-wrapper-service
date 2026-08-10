package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetLocationMetadataUseCaseTest {

    private LocationMetadataProvider metadataProvider;
    private GetSunTimesUseCase getSunTimes;
    private LocationResolver locationResolver;
    private GetLocationMetadataUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final WeatherOffice office = new WeatherOffice(
            "LWX", "NWS Baltimore/Washington", "KLWX",
            "America/New_York", "https://www.weather.gov/lwx");
    private final SunTimes sun = new SunTimes(
            LocalDate.of(2026, 8, 9),
            Instant.parse("2026-08-09T10:42:00Z"),
            Instant.parse("2026-08-10T00:34:00Z"),
            "America/New_York");

    @BeforeEach
    void setUp() {
        metadataProvider = mock(LocationMetadataProvider.class);
        getSunTimes = mock(GetSunTimesUseCase.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve("Arlington, VA")).thenReturn(arlington);
        useCase = new GetLocationMetadataUseCase(metadataProvider, getSunTimes, locationResolver);
    }

    @Test
    void resolvesCityThenFetchesOfficeAndSunTimes() {
        when(metadataProvider.getOfficeFor(arlington)).thenReturn(Optional.of(office));
        when(getSunTimes.execute("Arlington, VA")).thenReturn(Optional.of(sun));

        LocationMetadataResult result = useCase.execute("Arlington, VA");

        assertThat(result.office()).isEqualTo(office);
        assertThat(result.sunTimes()).contains(sun);
    }

    @Test
    void emptySunTimesIsToleratedButEmptyOfficeThrows() {
        when(metadataProvider.getOfficeFor(arlington)).thenReturn(Optional.of(office));
        when(getSunTimes.execute("Arlington, VA")).thenReturn(Optional.empty());

        LocationMetadataResult result = useCase.execute("Arlington, VA");

        assertThat(result.sunTimes()).isEmpty();
        assertThat(result.office()).isEqualTo(office);
    }

    @Test
    void emptyOfficePropagatesLocationNotFound() {
        when(metadataProvider.getOfficeFor(arlington)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("Arlington, VA"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(getSunTimes, never()).execute(anyString());
    }

    @Test
    void resolverMissPropagatesBeforeProviderCall() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(metadataProvider, never()).getOfficeFor(org.mockito.ArgumentMatchers.any());
    }
}
