package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NwsHourlyWeatherProviderTest {

    static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("weather.provider.base-url", wireMock::baseUrl);
        registry.add("weather.provider.timeout", () -> "5s");
    }

    @AfterAll
    static void teardown() {
        wireMock.stop();
    }

    @Autowired
    NwsHourlyWeatherProvider provider;

    private final Location location = new Location(38.8816, -77.0910, "Arlington, VA");

    @Test
    void fetchesHourlyForecastViaTwoStepFlow() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": "LWX", "gridX": 11, "gridY": 22 } }
                                """)));

        wireMock.stubFor(get(urlMatching("/gridpoints/LWX/.*/forecast/hourly"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "generatedAt": "2026-08-08T19:00:00+00:00",
                                    "periods": [
                                      {
                                        "startTime": "2026-08-08T19:00:00+00:00",
                                        "temperature": 87,
                                        "temperatureUnit": "F",
                                        "windSpeed": "5 mph",
                                        "windDirection": "NW",
                                        "shortForecast": "Sunny",
                                        "isDaytime": true
                                      },
                                      {
                                        "startTime": "2026-08-08T20:00:00+00:00",
                                        "temperature": 84,
                                        "temperatureUnit": "F",
                                        "windSpeed": "3 mph",
                                        "windDirection": "N",
                                        "shortForecast": "Mostly Clear",
                                        "isDaytime": false
                                      }
                                    ]
                                  }
                                }
                                """)));

        HourlyForecast fc = provider.getHourlyForecast(location)
                .orElseThrow(() -> new AssertionError("expected forecast"));

        assertThat(fc.source()).isEqualTo("National Weather Service (api.weather.gov)");
        assertThat(fc.periods()).hasSize(2);
        assertThat(fc.periods().get(0).startTime()).isNotNull();
        assertThat(fc.periods().get(0).temperature().value()).isEqualTo(87);
        assertThat(fc.periods().get(0).temperature().unit().name()).isEqualTo("FAHRENHEIT");
        assertThat(fc.periods().get(0).daytime()).isTrue();
        assertThat(fc.periods().get(1).daytime()).isFalse();
    }

    @Test
    void pointsWithoutGridIdThrowsWeatherProviderUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": null, "gridX": 11, "gridY": 22 } }
                                """)));

        assertThatThrownBy(() -> provider.getHourlyForecast(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("no gridId");
    }

    @Test
    void emptyHourlyResponseThrowsWeatherProviderUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": "LWX", "gridX": 11, "gridY": 22 } }
                                """)));

        wireMock.stubFor(get(urlMatching("/gridpoints/LWX/.*/forecast/hourly"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "generatedAt": "2026-08-08T19:00:00+00:00", "periods": [] } }
                                """)));

        assertThatThrownBy(() -> provider.getHourlyForecast(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("empty body");
    }
}