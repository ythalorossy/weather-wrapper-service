package io.ythalorossy.weatherapi.api;

import com.redis.testcontainers.RedisContainer;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.AlertCategory;
import io.ythalorossy.weatherapi.domain.model.AlertCertainty;
import io.ythalorossy.weatherapi.domain.model.AlertSeverity;
import io.ythalorossy.weatherapi.domain.model.AlertUrgency;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.HourlyForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.model.WeatherOffice;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;
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

import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;
import java.time.Instant;
import java.time.LocalDate;
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
        registry.add("weather.geocoding-base-url", () -> "http://localhost:0");
        registry.add("weather.provider-base-url", () -> "http://localhost:0");
        registry.add("weather.geocoding-timeout", () -> "100ms");
        registry.add("weather.provider-timeout", () -> "100ms");
        // Disable rate limiting in tests so multi-request tests don't hit the
        // burst bucket. Production keeps it on.
        registry.add("weather.rate-limit.enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @MockBean
    GeocodingProvider geocodingProvider;

    @MockBean
    WeatherProvider weatherProvider;

    @MockBean
    Cache<WeatherForecast> weatherCache;

    @MockBean
    Cache<Location> locationCache;

    @MockBean
    HourlyWeatherProvider hourlyWeatherProvider;

    @MockBean
    Cache<HourlyForecast> hourlyForecastCache;

    @MockBean
    LocationMetadataProvider locationMetadataProvider;

    @MockBean
    ObservationProvider observationProvider;

    @MockBean
    Cache<Observation> observationCache;

    @MockBean
    AlertProvider alertProvider;

    @MockBean
    Cache<List<WeatherAlert>> alertCache;

    @MockBean
    SunTimesProvider sunTimesProvider;

    @MockBean
    Cache<SunTimes> sunTimesCache;

    @MockBean
    AreaForecastDiscussionProvider afdProvider;

    @MockBean
    Cache<AfdProduct> afdCache;

    private static final String CITY = "Arlington, VA";
    private static final String GEO_KEY = "geo:arlington, va";
    private static final String CACHE_KEY = "38.8816,-77.0910";
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
        when(hourlyForecastCache.get(anyString())).thenReturn(Optional.empty());
        when(observationCache.get(anyString())).thenReturn(Optional.empty());
        when(alertCache.get(anyString())).thenReturn(Optional.empty());
        when(sunTimesCache.get(any())).thenReturn(Optional.empty());
        when(sunTimesProvider.getSunTimes(any(), any(), any())).thenReturn(Optional.empty());
        when(afdProvider.getLatest(anyString())).thenReturn(Optional.empty());
        when(afdCache.get(anyString())).thenReturn(Optional.empty());
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
    void getWeatherReturns200OnGeoCacheHitOnly() throws Exception {
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

    // ----- Hourly forecast endpoint -----

    private final HourlyForecast hourly = new HourlyForecast(
            List.of(new HourlyForecastPeriod(
                    Instant.parse("2026-08-08T19:00:00Z"),
                    Temperature.fahrenheit(85),
                    "5 mph", "NW", "Sunny", true)),
            Instant.parse("2026-08-08T18:30:00Z"),
            "National Weather Service (api.weather.gov)"
    );

    @Test
    void getHourlyForecastReturns200WithFullPayload() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(hourlyWeatherProvider.getHourlyForecast(location)).thenReturn(Optional.of(hourly));

        mvc.perform(get("/api/v1/weather/hourly").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(CITY))
                .andExpect(jsonPath("$.resolvedLocation.latitude").value(38.8816))
                .andExpect(jsonPath("$.forecast.source").value("National Weather Service (api.weather.gov)"))
                .andExpect(jsonPath("$.forecast.periods[0].startTime").value("2026-08-08T19:00:00Z"))
                .andExpect(jsonPath("$.forecast.periods[0].temperature.value").value(85))
                .andExpect(jsonPath("$.forecast.periods[0].temperature.unit").value("FAHRENHEIT"))
                .andExpect(jsonPath("$.forecast.periods[0].shortForecast").value("Sunny"))
                .andExpect(jsonPath("$.forecast.periods[0].daytime").value(true));
    }

    @Test
    void getHourlyForecastReturnsHourlyCacheHitWithoutCallingProvider() throws Exception {
        when(locationCache.get(GEO_KEY)).thenReturn(Optional.of(location));
        when(weatherCache.get("38.8816,-77.0910")).thenReturn(Optional.of(forecast));
        when(hourlyForecastCache.get("38.8816,-77.0910")).thenReturn(Optional.of(hourly));

        mvc.perform(get("/api/v1/weather/hourly").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.periods[0].startTime").value("2026-08-08T19:00:00Z"));

        verify(hourlyWeatherProvider, times(0)).getHourlyForecast(any());
    }

    @Test
    void getHourlyForecastBlankCityReturns400() throws Exception {
        mvc.perform(get("/api/v1/weather/hourly").param("city", "   "))
                .andExpect(status().isBadRequest());
    }

    // ----- Location metadata endpoint -----

    private final WeatherOffice office = new WeatherOffice(
            "LWX",
            "NWS Baltimore/Washington",
            "KLWX",
            "America/New_York",
            "https://www.weather.gov/lwx"
    );

    @Test
    void getMetadataReturns200WithOfficeDetails() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(locationMetadataProvider.getOfficeFor(any())).thenReturn(Optional.of(office));

        mvc.perform(get("/api/v1/weather/metadata").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(CITY))
                .andExpect(jsonPath("$.office.officeId").value("LWX"))
                .andExpect(jsonPath("$.office.name").value("NWS Baltimore/Washington"))
                .andExpect(jsonPath("$.office.radarStationId").value("KLWX"))
                .andExpect(jsonPath("$.office.timezoneId").value("America/New_York"))
                .andExpect(jsonPath("$.office.forecastOfficeUrl").value("https://www.weather.gov/lwx"));
    }

    @Test
    void getMetadataBlankCityReturns400() throws Exception {
        mvc.perform(get("/api/v1/weather/metadata").param("city", "   "))
                .andExpect(status().isBadRequest());
    }

    // ----- Conditions endpoint -----

    private final Observation observation = new Observation(
            "KIAD",
            "Washington/Dulles International Airport, VA",
            Instant.parse("2026-08-08T21:35:00Z"),
            78.4, 71.6, 5.2, 315, "NW", 83.5, 30.02, "Cloudy"
    );

    @Test
    void getConditionsReturns200WithObservation() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(observationProvider.getCurrentObservation(location)).thenReturn(Optional.of(observation));

        mvc.perform(get("/api/v1/conditions").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(CITY))
                .andExpect(jsonPath("$.resolvedLocation.latitude").value(38.8816))
                .andExpect(jsonPath("$.observation.stationId").value("KIAD"))
                .andExpect(jsonPath("$.observation.stationName").value(
                        "Washington/Dulles International Airport, VA"))
                .andExpect(jsonPath("$.observation.temperatureFahrenheit").value(78.4))
                .andExpect(jsonPath("$.observation.windSpeedMph").value(5.2))
                .andExpect(jsonPath("$.observation.windDirectionCompass").value("NW"))
                .andExpect(jsonPath("$.observation.textDescription").value("Cloudy"));
    }

    @Test
    void getConditionsBlankCityReturns400() throws Exception {
        mvc.perform(get("/api/v1/conditions").param("city", "   "))
                .andExpect(status().isBadRequest());
    }

    // ----- Alerts endpoint -----

    private final WeatherAlert alert = new WeatherAlert(
            "urn:oid:2.49.0.1.test1",
            "Severe Thunderstorm Warning",
            AlertSeverity.Severe,
            AlertCertainty.Likely,
            AlertUrgency.Expected,
            AlertCategory.Met,
            "Severe Thunderstorm Warning issued August 8",
            "Long description...",
            "Take shelter.",
            "Arlington County",
            Instant.parse("2026-08-08T21:53:00-04:00"),
            Instant.parse("2026-08-08T21:53:00-04:00"),
            Instant.parse("2026-08-08T22:30:00-04:00"),
            "https://example.com"
    );

    @Test
    void getAlertsReturns200WithAlertsList() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(alertProvider.getActiveAlerts(location)).thenReturn(List.of(alert));

        mvc.perform(get("/api/v1/alerts").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(CITY))
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.alerts[0].event").value("Severe Thunderstorm Warning"))
                .andExpect(jsonPath("$.alerts[0].severity").value("Severe"))
                .andExpect(jsonPath("$.alerts[0].areaDesc").value("Arlington County"));
    }

    @Test
    void getAlertsReturns200WithEmptyListWhenNoActiveAlerts() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(alertProvider.getActiveAlerts(location)).thenReturn(List.of());

        mvc.perform(get("/api/v1/alerts").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.alerts.length()").value(0));
    }

    @Test
    void getAlertsBlankCityReturns400() throws Exception {
        mvc.perform(get("/api/v1/alerts").param("city", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void metadataIncludesSunViewWhenProviderReturnsSunTimes() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(locationMetadataProvider.getOfficeFor(any())).thenReturn(Optional.of(office));
        when(sunTimesProvider.getSunTimes(any(), any(), any()))
                .thenReturn(Optional.of(new SunTimes(
                        LocalDate.of(2026, 8, 9),
                        Instant.parse("2026-08-09T10:42:00Z"),
                        Instant.parse("2026-08-10T00:34:00Z"),
                        "America/New_York")));

        mvc.perform(get("/api/v1/weather/metadata").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sun").exists())
                .andExpect(jsonPath("$.sun.date").value("2026-08-09"))
                .andExpect(jsonPath("$.sun.sunriseLocal").value("06:42"))
                .andExpect(jsonPath("$.sun.sunsetLocal").value("20:34"))
                .andExpect(jsonPath("$.sun.dayLengthSeconds").value(49920));
    }

    @Test
    void metadataOmitsSunWhenProviderReturnsEmpty() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(weatherProvider.getForecast(location)).thenReturn(forecast);
        when(locationMetadataProvider.getOfficeFor(any())).thenReturn(Optional.of(office));
        when(sunTimesProvider.getSunTimes(any(), any(), any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/weather/metadata").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sun").doesNotExist());
    }

    // ----- Discussion endpoint -----

    @Test
    void discussionReturnsBodyForKnownCity() throws Exception {
        AfdProduct product = new AfdProduct(
                "LWX",
                Instant.parse("2026-08-10T14:35:00Z"),
                "KLWX AFD\n\n.SHORT TERM...\n\nDry weather through Tuesday.");
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(locationMetadataProvider.getOfficeFor(any())).thenReturn(Optional.of(office));
        when(afdProvider.getLatest("LWX")).thenReturn(Optional.of(product));

        mvc.perform(get("/api/v1/weather/forecast/discussion").param("city", CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officeId").value("LWX"))
                .andExpect(jsonPath("$.issuanceTime").value("2026-08-10T14:35:00Z"))
                .andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString("Dry weather")));
    }

    @Test
    void discussionReturns404WhenProviderReturnsEmpty() throws Exception {
        when(geocodingProvider.findLocation(CITY)).thenReturn(Optional.of(location));
        when(locationMetadataProvider.getOfficeFor(any())).thenReturn(Optional.of(office));
        when(afdProvider.getLatest("LWX")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/weather/forecast/discussion").param("city", CITY))
                .andExpect(status().isNotFound());
    }

    @Test
    void discussionReturns400WhenCityBlank() throws Exception {
        mvc.perform(get("/api/v1/weather/forecast/discussion").param("city", "  "))
                .andExpect(status().isBadRequest());
    }
}