package io.ythalorossy.weatherapi.api.controller;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.application.usecase.GetStationObservationsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetStationsUseCase;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StationControllerTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void disableUpstreamCalls(DynamicPropertyRegistry registry) {
        registry.add("weather.geocoding-base-url", () -> "http://localhost:0");
        registry.add("weather.provider-base-url", () -> "http://localhost:0");
        registry.add("weather.geocoding-timeout", () -> "100ms");
        registry.add("weather.provider-timeout", () -> "100ms");
        registry.add("weather.rate-limit.enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @MockBean
    GetStationsUseCase getStationsUseCase;

    @MockBean
    GetStationObservationsUseCase getStationObservationsUseCase;

    private final Station station = new Station("KDCA", "National", 38.85, -77.04);
    private final StationObservation observation = new StationObservation(
            "KDCA",
            Instant.parse("2026-08-13T15:00:00Z"),
            Temperature.fahrenheit(82),
            55,
            "6 mph", "NW",
            "KDCA 131553Z 31006KT 10SM FEW060 SCT120 28/12 A3005",
            30.04
    );

    @Test
    void getStationsEndpointReturnsList() throws Exception {
        when(getStationsUseCase.execute("Arlington, VA"))
                .thenReturn(List.of(station));

        mvc.perform(get("/api/v1/weather/stations").param("city", "Arlington, VA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationId").value("KDCA"))
                .andExpect(jsonPath("$[0].name").value("National"))
                .andExpect(jsonPath("$[0].latitude").value(38.85))
                .andExpect(jsonPath("$[0].longitude").value(-77.04));
    }

    @Test
    void getStationsBlankCityReturns400() throws Exception {
        mvc.perform(get("/api/v1/weather/stations").param("city", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getObservationsEndpointReturnsList() throws Exception {
        when(getStationObservationsUseCase.execute(anyString()))
                .thenReturn(List.of(observation));

        mvc.perform(get("/api/v1/weather/stations/KDCA/observations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationId").value("KDCA"))
                .andExpect(jsonPath("$[0].temperature.value").value(82))
                .andExpect(jsonPath("$[0].humidity").value(55));
    }

    @Test
    void getObservationsBlankStationIdReturns400() throws Exception {
        // Use case rejects blank station id; GlobalExceptionHandler maps it to 400.
        when(getStationObservationsUseCase.execute(anyString()))
                .thenThrow(new IllegalArgumentException("stationId must not be blank"));

        mvc.perform(get("/api/v1/weather/stations/KDCA/observations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
