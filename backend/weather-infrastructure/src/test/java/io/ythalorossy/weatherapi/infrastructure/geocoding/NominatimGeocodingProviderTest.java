package io.ythalorossy.weatherapi.infrastructure.geocoding;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NominatimGeocodingProviderTest {

    static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("weather.geocoding.base-url", wireMock::baseUrl);
    }

    @AfterAll
    static void teardown() {
        wireMock.stop();
    }

    @Autowired
    NominatimGeocodingProvider provider;

    @Test
    void resolvesCityToLocation() {
        wireMock.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("Arlington, VA"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{
                                  "place_id": 12345,
                                  "lat": "38.8816066",
                                  "lon": "-77.0910123",
                                  "display_name": "Arlington, Arlington County, Virginia, United States"
                                }]
                                """)));

        Optional<Location> result = provider.findLocation("Arlington, VA");

        assertThat(result).isPresent();
        assertThat(result.get().latitude()).isEqualTo(38.8816066);
        assertThat(result.get().longitude()).isEqualTo(-77.0910123);
        assertThat(result.get().displayName()).contains("Arlington");
    }

    @Test
    void unknownCityReturnsEmpty() {
        wireMock.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("NowhereVille"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        assertThat(provider.findLocation("NowhereVille")).isEmpty();
    }

    @Test
    void invalidLatLonReturnsEmpty() {
        wireMock.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("GlitchTown"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{
                                  "lat": "not-a-number",
                                  "lon": "still-not-a-number",
                                  "display_name": "GlitchTown"
                                }]
                                """)));

        assertThat(provider.findLocation("GlitchTown")).isEmpty();
    }

    @Test
    void blankCityRejected() {
        assertThatThrownBy(() -> provider.findLocation(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.findLocation("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullCityRejected() {
        assertThatThrownBy(() -> provider.findLocation(null))
                .isInstanceOf(NullPointerException.class);
    }
}