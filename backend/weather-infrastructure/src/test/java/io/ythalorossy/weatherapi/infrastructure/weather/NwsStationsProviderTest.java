package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NwsStationsProviderTest {

    static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("weather.provider-base-url", wireMock::baseUrl);
    }

    @AfterAll
    static void teardown() {
        wireMock.stop();
    }

    @Autowired
    NwsStationsProvider provider;

    @Test
    void fetchesStationsList() {
        wireMock.stubFor(get(urlPathEqualTo("/gridpoints/LWX/11,22/stations"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "features": [
                                    {
                                      "properties": {
                                        "stationIdentifier": "KDCA",
                                        "name": "Washington National"
                                      },
                                      "geometry": {
                                        "coordinates": [-77.04, 38.85]
                                      }
                                    }
                                  ]
                                }
                                """)));

        List<Station> stations = provider.getStations("LWX", 11, 22);
        assertThat(stations).hasSize(1);
        assertThat(stations.get(0).stationId()).isEqualTo("KDCA");
        assertThat(stations.get(0).latitude()).isEqualTo(38.85);
        assertThat(stations.get(0).longitude()).isEqualTo(-77.04);
    }
}
