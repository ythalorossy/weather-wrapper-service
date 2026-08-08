package io.ythalorossy.weatherapi.api;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.LocationCache;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WeatherApiApplicationTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    /**
     * Replace the real {@code weather.geocoding.base-url} with a localhost URL.
     * The {@code GeocodingProvider} bean is mocked via {@link MockBean}, so
     * Spring Boot's {@code RestClient} autoconfig never actually calls out.
     */
    @DynamicPropertySource
    static void disableUpstreamCalls(DynamicPropertyRegistry registry) {
        registry.add("weather.geocoding.base-url", () -> "http://localhost:0");
        registry.add("weather.provider.base-url", () -> "http://localhost:0");
        registry.add("weather.geocoding.timeout", () -> "100ms");
        registry.add("weather.provider.timeout", () -> "100ms");
    }

    @Autowired
    MockMvc mvc;

    @MockBean
    GeocodingProvider geocodingProvider;

    @MockBean
    WeatherProvider weatherProvider;

    @MockBean
    WeatherCache weatherCache;

    @MockBean
    LocationCache locationCache;

    private static final String CITY = "Arlington, VA";
    private static final String GEO_KEY = "geo:arlington, va";
    private static final String CACHE_KEY = "weather:38.88,-77.09";
    private final Location location = new Location(38.8816, -77.0910, "Arlington, Arlington County, Virginia, United States");
    private final WeatherForecast forecast = new WeatherForecast(
            List.of(new ForecastPeriod("Today", Temperature.fahrenheit(85), "5 mph", "NW",
                    "Sunny", "Sunny, with a high near 85.", true)),
            Instant.parse("2026-08-07T12:00:00Z"),
            "National Weather Service (api.weather.gov)"
    );

    @BeforeEach
    void defaultCacheBehavior() {
        // Both caches miss by default; tests override as needed.
        when(locationCache.get(anyString())).thenReturn(Optional.empty());
        when(weatherCache.get(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void contextLoads() {
        // Verifies the full Spring context (component scan, configuration properties,
        // RestClient beans, use case wiring) wires up without error.
    }

    @Test
    void getWeatherReturns200WithFullPayloadOnCacheMiss() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherCache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(weatherProvider.getForecast(location)).thenReturn(forecast);

        mvc.perform(get("/api/v1/weather").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(CITY))
                .andExpect(jsonPath("$.resolvedLocation.latitude").value(38.8816))
                .andExpect(jsonPath("$.resolvedLocation.longitude").value(-77.0910))
                .andExpect(jsonPath("$.resolvedLocation.displayName").value(
                        "Arlington, Arlington County, Virginia, United States"))
                .andExpect(jsonPath("$.forecast.source").value("National Weather Service (api.weather.gov)"))
                .andExpect(jsonPath("$.forecast.periods[0].name").value("Today"))
                .andExpect(jsonPath("$.forecast.periods[0].temperature.value").value(85))
                .andExpect(jsonPath("$.forecast.periods[0].temperature.unit").value("FAHRENHEIT"))
                .andExpect(jsonPath("$.forecast.periods[0].temperature.formatted").value("85°F"))
                .andExpect(jsonPath("$.forecast.periods[0].shortForecast").value("Sunny"))
                .andExpect(jsonPath("$.forecast.periods[0].daytime").value(true));
    }

    @Test
    void getWeatherReturns200OnCacheHit() throws Exception {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.of(location));
        when(weatherCache.get(CACHE_KEY)).thenReturn(Optional.of(forecast));

        mvc.perform(get("/api/v1/weather").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.periods[0].name").value("Today"));
    }

    @Test
    void getWeatherReturns200OnLocationCacheHitOnly() throws Exception {
        // Location cached, weather not — use case should still resolve location from cache
        // and only hit NWS for the forecast.
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);

        mvc.perform(get("/api/v1/weather").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedLocation.displayName").value(location.displayName()))
                .andExpect(jsonPath("$.forecast.periods[0].name").value("Today"));
    }

    @Test
    void unknownCityReturns404WithProblemDetail() throws Exception {
        when(geocodingProvider.findLocation("NowhereVille")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/weather").param("city", "NowhereVille"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Location not found"))
                .andExpect(jsonPath("$.city").value("NowhereVille"));
    }

    @Test
    void secondRequestForUnknownCityUsesAbsentCache() throws Exception {
        // Simulate the markAbsent→isAbsent flow: first call to isAbsent returns
        // false (not yet cached as absent), second returns true (post-markAbsent).
        AtomicBoolean isAbsentFlip = new AtomicBoolean(false);
        when(locationCache.isAbsent("geo:nowhereville"))
                .thenAnswer(inv -> isAbsentFlip.getAndSet(true));
        when(geocodingProvider.findLocation("NowhereVille")).thenReturn(Optional.empty());

        // First request: 404 + geocoder hit
        mvc.perform(get("/api/v1/weather").param("city", "NowhereVille"))
                .andExpect(status().isNotFound());

        // Second request: 404 from absent-cache hit, no geocoder call
        mvc.perform(get("/api/v1/weather").param("city", "NowhereVille"))
                .andExpect(status().isNotFound());

        // Geocoder must have been called exactly once across both requests
        verify(geocodingProvider, times(1)).findLocation("NowhereVille");
        verify(locationCache).markAbsent(eq("geo:nowhereville"), any());
    }

    @Test
    void cityVariationsShareCacheHit() throws Exception {
        // "Arlington, VA" and "arlington, va" should both hit the same cache entry
        // (whitespace+case normalization). Pre-populate the cache with the lowercased key.
        when(locationCache.get("geo:arlington, va")).thenReturn(Optional.of(location));
        when(weatherCache.get(CACHE_KEY)).thenReturn(Optional.of(forecast));

        mvc.perform(get("/api/v1/weather").param("city", "  Arlington,  VA  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.periods[0].name").value("Today"));
    }

    @Test
    void upstreamUnavailableReturns502() throws Exception {
        when(geocodingProvider.findLocation("Arlington")).thenReturn(Optional.of(location));
        when(weatherCache.get(anyString())).thenReturn(Optional.empty());
        when(weatherProvider.getForecast(any()))
                .thenThrow(new WeatherProviderUnavailableException("NWS down for maintenance"));

        mvc.perform(get("/api/v1/weather").param("city", "Arlington"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Upstream weather provider unavailable"));
    }

    @Test
    void missingCityParamReturns400() throws Exception {
        mvc.perform(get("/api/v1/weather"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankCityParamReturns400() throws Exception {
        mvc.perform(get("/api/v1/weather").param("city", "   "))
                .andExpect(status().isBadRequest());
    }
}