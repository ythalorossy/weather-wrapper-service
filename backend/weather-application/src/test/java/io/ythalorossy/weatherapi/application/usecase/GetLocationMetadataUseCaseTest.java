package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private LocationResolver locationResolver;
    private GetLocationMetadataUseCase useCase;

    private final Location arlington = new Location(38.8816, -77.0910, "Arlington, VA");
    private final WeatherOffice office = new WeatherOffice(
            "LWX",
            "NWS Baltimore/Washington",
            "KLWX",
            "America/New_York",
            "https://www.weather.gov/lwx"
    );

    @BeforeEach
    void setUp() {
        metadataProvider = mock(LocationMetadataProvider.class);
        locationResolver = mock(LocationResolver.class);
        when(locationResolver.resolve("Arlington, VA")).thenReturn(arlington);
        useCase = new GetLocationMetadataUseCase(metadataProvider, locationResolver);
    }

    @Test
    void resolvesCityThenFetchesOfficeFromProvider() {
        when(metadataProvider.getOfficeFor(arlington)).thenReturn(Optional.of(office));

        Optional<WeatherOffice> result = useCase.execute("Arlington, VA");

        assertThat(result).contains(office);
        verify(metadataProvider).getOfficeFor(arlington);
    }

    @Test
    void emptyResultWhenProviderReturnsEmpty() {
        when(metadataProvider.getOfficeFor(arlington)).thenReturn(Optional.empty());

        Optional<WeatherOffice> result = useCase.execute("Arlington, VA");

        assertThat(result).isEmpty();
    }

    @Test
    void locationNotFoundFromResolverPropagates() {
        when(locationResolver.resolve("NowhereVille"))
                .thenThrow(new io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException("NowhereVille"));

        assertThatThrownBy(() -> useCase.execute("NowhereVille"))
                .isInstanceOf(io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException.class);

        verify(metadataProvider, never()).getOfficeFor(org.mockito.ArgumentMatchers.any());
    }
}