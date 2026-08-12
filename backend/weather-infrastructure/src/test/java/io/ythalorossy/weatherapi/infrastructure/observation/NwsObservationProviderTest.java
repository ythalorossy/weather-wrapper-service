package io.ythalorossy.weatherapi.infrastructure.observation;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import static org.assertj.core.api.Assertions.within;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NwsObservationProviderTest {

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
    NwsObservationProvider provider;

    @Test
    void twoStepFlowFindsNearestStationAndFetchesLatestObservation() {
        wireMock.stubFor(get(urlMatching("/points/.*/stations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "features": [
                                    {
                                      "properties": {
                                        "stationIdentifier": "KIAD",
                                        "name": "Washington/Dulles International Airport, VA",
                                        "latitude": 38.9445,
                                        "longitude": -77.4558
                                      }
                                    },
                                    {
                                      "properties": {
                                        "stationIdentifier": "KDCA",
                                        "name": "Washington/Reagan National Airport, DC",
                                        "latitude": 38.8512,
                                        "longitude": -77.0402
                                      }
                                    }
                                  ]
                                }
                                """)));

        wireMock.stubFor(get(urlMatching("/stations/KIAD/observations/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "timestamp": "2026-08-08T21:35:00+00:00",
                                    "textDescription": "Cloudy",
                                    "temperature": { "value": 25, "unitCode": "wmoUnit:degC" },
                                    "dewpoint":    { "value": 22, "unitCode": "wmoUnit:degC" },
                                    "windDirection": { "value": 0, "unitCode": "wmoUnit:degree_(angle)" },
                                    "windSpeed":     { "value": 8, "unitCode": "wmoUnit:km_h-1" },
                                    "barometricPressure": { "value": 101625, "unitCode": "wmoUnit:Pa" },
                                    "relativeHumidity": { "value": 83.45, "unitCode": "wmoUnit:percent" }
                                  }
                                }
                                """)));

        Observation obs = provider.getCurrentObservation(
                new io.ythalorossy.weatherapi.domain.model.Location(38.88, -77.09, "Arlington, VA")
        ).orElseThrow(() -> new AssertionError("expected observation"));

        // Asserts: SI -> consumer units
        assertThat(obs.stationId()).isEqualTo("KIAD");
        assertThat(obs.stationName()).contains("Dulles");
        assertThat(obs.temperatureFahrenheit()).isEqualTo(77.0);            // 25 C -> 77 F
        assertThat(obs.dewpointFahrenheit()).isEqualTo(71.6);                // 22 C -> 71.6 F
        assertThat(obs.windSpeedMph()).isCloseTo(4.97, within(0.01));        // 8 km/h -> 4.970968 mph
        assertThat(obs.windDirectionDegrees()).isEqualTo(0);
        assertThat(obs.windDirectionCompass()).isEqualTo("N");                // 0 degrees -> N
        assertThat(obs.relativeHumidityPercent()).isEqualTo(83.45);
        assertThat(obs.barometricPressureInHg()).isCloseTo(30.00, within(0.01)); // 101625 Pa -> ~30 inHg
        assertThat(obs.textDescription()).isEqualTo("Cloudy");
    }

    @Test
    void emptyStationsListReturnsEmpty() {
        wireMock.stubFor(get(urlMatching("/points/.*/stations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "features": [] }
                                """)));

        var result = provider.getCurrentObservation(
                new io.ythalorossy.weatherapi.domain.model.Location(38.88, -77.09, "Arlington, VA")
        );

        assertThat(result).isEmpty();
    }
}