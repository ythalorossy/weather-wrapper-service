package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NwsAfdProviderTest {

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

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
    }

    @Autowired
    NwsAfdProvider provider;

    @Test
    void returnsLatestProductMatchingOfficeId() {
        wireMock.stubFor(get(urlMatching("/products/types/AFD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/ld+json")
                        .withBody("""
                                {
                                  "@graph": [
                                    {
                                      "id": "https://api.weather.gov/products/kbox-1",
                                      "issuingOffice": "KBOX",
                                      "issuanceTime": "2026-08-10T10:00:00+00:00",
                                      "productCode": "AFD",
                                      "productName": "Area Forecast Discussion"
                                    },
                                    {
                                      "id": "https://api.weather.gov/products/klwx-1",
                                      "issuingOffice": "KLWX",
                                      "issuanceTime": "2026-08-10T14:35:00+00:00",
                                      "productCode": "AFD",
                                      "productName": "Area Forecast Discussion"
                                    },
                                    {
                                      "id": "https://api.weather.gov/products/klwx-2",
                                      "issuingOffice": "KLWX",
                                      "issuanceTime": "2026-08-10T11:29:00+00:00",
                                      "productCode": "AFD",
                                      "productName": "Area Forecast Discussion"
                                    }
                                  ]
                                }
                                """)));

        wireMock.stubFor(get(urlMatching("/products/klwx-1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/ld+json")
                        .withBody("""
                                {
                                  "id": "https://api.weather.gov/products/klwx-1",
                                  "issuingOffice": "KLWX",
                                  "issuanceTime": "2026-08-10T14:35:00+00:00",
                                  "productCode": "AFD",
                                  "productText": "KLWX AFD\\n\\n.SHORT TERM...\\n\\nDry weather through Tuesday."
                                }
                                """)));

        Optional<AfdProduct> result = provider.getLatest("KLWX");

        assertThat(result).isPresent();
        AfdProduct product = result.get();
        assertThat(product.officeId()).isEqualTo("KLWX");
        assertThat(product.issuanceTime()).isEqualTo(Instant.parse("2026-08-10T14:35:00Z"));
        assertThat(product.body()).contains("Dry weather through Tuesday.");
    }

    @Test
    void returnsEmptyWhenOfficeHasNoMatchingProduct() {
        wireMock.stubFor(get(urlMatching("/products/types/AFD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/ld+json")
                        .withBody("""
                                {
                                  "@graph": [
                                    {
                                      "id": "https://api.weather.gov/products/kbox-1",
                                      "issuingOffice": "KBOX",
                                      "issuanceTime": "2026-08-10T10:00:00+00:00",
                                      "productCode": "AFD",
                                      "productName": "Area Forecast Discussion"
                                    }
                                  ]
                                }
                                """)));

        assertThat(provider.getLatest("KLWX")).isEmpty();
    }

    @Test
    void returnsEmptyWhenListEndpointFails() {
        wireMock.stubFor(get(urlMatching("/products/types/AFD"))
                .willReturn(aResponse().withStatus(503)));

        assertThat(provider.getLatest("KLWX")).isEmpty();
    }

    @Test
    void returnsEmptyWhenBodyEndpointFails() {
        wireMock.stubFor(get(urlMatching("/products/types/AFD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/ld+json")
                        .withBody("""
                                {
                                  "@graph": [
                                    {
                                      "id": "https://api.weather.gov/products/klwx-1",
                                      "issuingOffice": "KLWX",
                                      "issuanceTime": "2026-08-10T14:35:00+00:00",
                                      "productCode": "AFD",
                                      "productName": "Area Forecast Discussion"
                                    }
                                  ]
                                }
                                """)));

        wireMock.stubFor(get(urlMatching("/products/klwx-1"))
                .willReturn(aResponse().withStatus(503)));

        assertThat(provider.getLatest("KLWX")).isEmpty();
    }

    @Test
    void blankOfficeIdThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> provider.getLatest("  "));
    }
}