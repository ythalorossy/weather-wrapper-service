package io.ythalorossy.weatherapi.infrastructure.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
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
class NwsWeatherProviderTest {

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
    NwsWeatherProvider provider;

    private final Location location = new Location(38.8816, -77.0910, "Arlington, VA");

    @Test
    void fetchesForecastViaTwoStepFlow() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": "LWX", "gridX": 11, "gridY": 22 } }
                                """)));

        wireMock.stubFor(get(urlMatching("/gridpoints/LWX/.*/forecast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "generatedAt": "2026-08-07T12:00:00+00:00",
                                    "periods": [
                                      {
                                        "name": "Today",
                                        "temperature": 85,
                                        "temperatureUnit": "F",
                                        "windSpeed": "5 mph",
                                        "windDirection": "NW",
                                        "shortForecast": "Sunny",
                                        "detailedForecast": "Sunny, with a high near 85.",
                                        "isDaytime": true
                                      },
                                      {
                                        "name": "Tonight",
                                        "temperature": 70,
                                        "temperatureUnit": "F",
                                        "windSpeed": "0 mph",
                                        "windDirection": "",
                                        "shortForecast": "Clear",
                                        "detailedForecast": "Clear, with a low around 70.",
                                        "isDaytime": false
                                      }
                                    ]
                                  }
                                }
                                """)));

        WeatherForecast fc = provider.getForecast(location);

        assertThat(fc.source()).isEqualTo("National Weather Service (api.weather.gov)");
        assertThat(fc.periods()).hasSize(2);
        assertThat(fc.periods().get(0).name()).isEqualTo("Today");
        assertThat(fc.periods().get(0).temperature().value()).isEqualTo(85);
        assertThat(fc.periods().get(0).temperature().unit().name()).isEqualTo("FAHRENHEIT");
        assertThat(fc.periods().get(1).daytime()).isFalse();
        assertThat(fc.generatedAt()).isNotNull();
    }

    @Test
    void parsesCelsiusTemperatureUnit() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": "LWX", "gridX": 11, "gridY": 22 } }
                                """)));

        wireMock.stubFor(get(urlMatching("/gridpoints/LWX/.*/forecast"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "properties": {
                                    "generatedAt": "2026-08-07T12:00:00+00:00",
                                    "periods": [{
                                      "name": "Today",
                                      "temperature": 29,
                                      "temperatureUnit": "C",
                                      "windSpeed": "5 mph",
                                      "windDirection": "NW",
                                      "shortForecast": "Sunny",
                                      "detailedForecast": "Sunny.",
                                      "isDaytime": true
                                    }]
                                  }
                                }
                                """)));

        WeatherForecast fc = provider.getForecast(location);
        assertThat(fc.periods().get(0).temperature().unit().name()).isEqualTo("CELSIUS");
        assertThat(fc.periods().get(0).temperature().formatted()).isEqualTo("29°C");
    }

    @Test
    void pointsFailureThrowsWeatherProviderUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> provider.getForecast(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("503");
    }

    @Test
    void forecastFailureThrowsWeatherProviderUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": "LWX", "gridX": 11, "gridY": 22 } }
                                """)));
        wireMock.stubFor(get(urlMatching("/gridpoints/LWX/.*/forecast"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> provider.getForecast(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void pointsWithoutGridIdThrowsWeatherProviderUnavailable() {
        wireMock.stubFor(get(urlMatching("/points/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "properties": { "gridId": null, "gridX": 11, "gridY": 22 } }
                                """)));

        assertThatThrownBy(() -> provider.getForecast(location))
                .isInstanceOf(WeatherProviderUnavailableException.class)
                .hasMessageContaining("gridId");
    }

    @Test
    void nullLocationRejected() {
        assertThatThrownBy(() -> provider.getForecast(null))
                .isInstanceOf(NullPointerException.class);
    }
}