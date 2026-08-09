package io.ythalorossy.weatherapi.infrastructure.observation;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfrastructureTestConfig.class)
@ContextConfiguration(classes = InfrastructureTestConfig.class)
class NwsAlertProviderTest {

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
    NwsAlertProvider provider;

    @Test
    void mapsAlertsResponseToDomain() {
        wireMock.stubFor(get(urlMatching("/alerts/active.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "features": [
                                    {
                                      "properties": {
                                        "id": "urn:oid:2.49.0.1.test1",
                                        "event": "Severe Thunderstorm Warning",
                                        "severity": "Severe",
                                        "certainty": "Likely",
                                        "urgency": "Expected",
                                        "category": "Met",
                                        "headline": "Severe Thunderstorm Warning issued August 8",
                                        "description": "At 530 PM EDT...",
                                        "instruction": "Move indoors.",
                                        "areaDesc": "Arlington County",
                                        "sent": "2026-08-08T21:53:00-04:00",
                                        "effective": "2026-08-08T21:53:00-04:00",
                                        "expires": "2026-08-08T22:30:00-04:00",
                                        "web": "https://alerts.weather.gov/..."
                                      }
                                    }
                                  ]
                                }
                                """)));

        List<WeatherAlert> alerts = provider.getActiveAlerts(
                new io.ythalorossy.weatherapi.domain.model.Location(38.88, -77.09, "Arlington, VA")
        );

        assertThat(alerts).hasSize(1);
        WeatherAlert a = alerts.get(0);
        assertThat(a.id()).isEqualTo("urn:oid:2.49.0.1.test1");
        assertThat(a.event()).isEqualTo("Severe Thunderstorm Warning");
        assertThat(a.severity().name()).isEqualTo("Severe");
        assertThat(a.certainty().name()).isEqualTo("Likely");
        assertThat(a.urgency().name()).isEqualTo("Expected");
        assertThat(a.category().name()).isEqualTo("Met");
        assertThat(a.headline()).startsWith("Severe Thunderstorm Warning");
        assertThat(a.instruction()).isEqualTo("Move indoors.");
        assertThat(a.areaDesc()).isEqualTo("Arlington County");
        assertThat(a.webUrl()).startsWith("https://alerts.weather.gov");
    }

    @Test
    void emptyFeaturesReturnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/alerts/active.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "features": [] }
                                """)));

        List<WeatherAlert> alerts = provider.getActiveAlerts(
                new io.ythalorossy.weatherapi.domain.model.Location(38.88, -77.09, "Arlington, VA")
        );

        assertThat(alerts).isEmpty();
    }
}