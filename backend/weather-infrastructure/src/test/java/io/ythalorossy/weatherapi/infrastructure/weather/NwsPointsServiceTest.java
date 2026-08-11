package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class NwsPointsServiceTest {

    static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void setup() { wireMock.start(); }

    @AfterAll
    static void teardown() { wireMock.stop(); }

    @Test
    void memoizesPointsLookupPerCoordinate() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            { "properties": { "gridId": "LWX", "gridX": 1, "gridY": 1 } }""")));

        NwsPointsService svc = new NwsPointsService(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        PointsResponse a = svc.lookup(38.88, -77.09);
        PointsResponse b = svc.lookup(38.88, -77.09);

        assertThat(a).isSameAs(b);
        wireMock.verify(1, getRequestedFor(urlMatching("/points/.*")));
    }
}