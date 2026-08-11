package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NwsLocationMetadataProviderTest {

    static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("weather.provider.base-url", wireMock::baseUrl);
    }

    @AfterAll
    static void teardown() {
        wireMock.stop();
    }

    @Autowired
    NwsLocationMetadataProvider provider;

    @Autowired
    NwsPointsService pointsService;

    private final Location location = new Location(38.8816, -77.0910, "Arlington, VA");

    @BeforeEach
    void resetMemo() {
        pointsService.clearMemo();
    }

    @Test
    void resolvesOfficeIdFromForecastOfficeUrlAndBuildsWeatherOffice() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "gridId": "LWX",
                                    "gridX": 11,
                                    "gridY": 22,
                                    "timeZone": "America/New_York",
                                    "radarStation": "KLWX",
                                    "forecastOffice": "https://api.weather.gov/offices/LWX"
                                  }
                                }
                                """)));

        wireMock.stubFor(get(urlMatching("/offices/LWX"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "name": "NWS Baltimore/Washington",
                                  "sameAs": "https://www.weather.gov/lwx"
                                }
                                """)));

        WeatherOffice office = provider.getOfficeFor(location)
                .orElseThrow(() -> new AssertionError("expected office"));

        assertThat(office.officeId()).isEqualTo("LWX");
        assertThat(office.name()).isEqualTo("NWS Baltimore/Washington");
        assertThat(office.radarStationId()).isEqualTo("KLWX");
        assertThat(office.timezoneId()).isEqualTo("America/New_York");
        assertThat(office.forecastOfficeUrl()).isEqualTo("https://www.weather.gov/lwx");
    }

    @Test
    void missingTimezoneOrRadarThrowsUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "gridId": "LWX",
                                    "gridX": 11,
                                    "gridY": 22,
                                    "forecastOffice": "https://api.weather.gov/offices/LWX"
                                  }
                                }
                                """)));

        assertThatThrownBy(() -> provider.getOfficeFor(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("timeZone or radarStation");
    }

    @Test
    void malformedForecastOfficeUrlThrowsUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "gridId": "LWX",
                                    "gridX": 11,
                                    "gridY": 22,
                                    "timeZone": "America/New_York",
                                    "radarStation": "KLWX",
                                    "forecastOffice": "not-a-url"
                                  }
                                }
                                """)));

        assertThatThrownBy(() -> provider.getOfficeFor(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("no usable forecastOffice URL");
    }

    @Test
    void emptyOfficeResponseThrowsUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "gridId": "LWX",
                                    "gridX": 11,
                                    "gridY": 22,
                                    "timeZone": "America/New_York",
                                    "radarStation": "KLWX",
                                    "forecastOffice": "https://api.weather.gov/offices/LWX"
                                  }
                                }
                                """)));

        wireMock.stubFor(get(urlMatching("/offices/LWX"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        assertThatThrownBy(() -> provider.getOfficeFor(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("empty body");
    }
}