package io.ythalorossy.weatherapi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.ythalorossy.weatherapi.api.dto.DiscussionResponse;
import io.ythalorossy.weatherapi.api.dto.HourlyWeatherResponse;
import io.ythalorossy.weatherapi.api.dto.LocationMetadataResponse;
import io.ythalorossy.weatherapi.api.dto.WeatherResponse;
import io.ythalorossy.weatherapi.application.usecase.GetAfdUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetHourlyForecastUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetLocationMetadataUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.application.usecase.WeatherQueryResult;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@Validated
@Tag(name = "Weather", description = "Forecast lookups for free-text city names.")
public class WeatherController {

    private final GetWeatherUseCase getWeather;
    private final GetHourlyForecastUseCase getHourlyWeather;
    private final GetLocationMetadataUseCase getLocationMetadata;
    private final GetAfdUseCase getAfd;

    public WeatherController(
            GetWeatherUseCase getWeather,
            GetHourlyForecastUseCase getHourlyWeather,
            GetLocationMetadataUseCase getLocationMetadata,
            GetAfdUseCase getAfd) {
        this.getWeather = getWeather;
        this.getHourlyWeather = getHourlyWeather;
        this.getLocationMetadata = getLocationMetadata;
        this.getAfd = getAfd;
    }

    @GetMapping
    @Operation(
            summary = "Get 12-hour-block forecast for a city",
            description = """
                    Resolves the city to coordinates via Nominatim, then fetches the
                    multi-period forecast from the National Weather Service. Results are
                    cached server-side (Redis) per coordinate rounded to ~1.1 km.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Forecast retrieved.",
                    content = @Content(schema = @Schema(implementation = WeatherResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "city is missing or blank.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404",
                    description = "City not found by Nominatim.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502",
                    description = "NWS unreachable or returned a non-success status.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<WeatherResponse> getWeather(
            @Parameter(description = """
                    Free-text city name, e.g. `Arlington, VA`. Normalized (trim + \
                    lower-case + whitespace-collapsed) before geocoding.""",
                    example = "Arlington, VA",
                    required = true)
            @RequestParam("city") @NotBlank String city) {
        WeatherQueryResult result = getWeather.execute(city);
        return ResponseEntity.ok(toResponse(city, result));
    }

    @GetMapping("/hourly")
    @Operation(
            summary = "Get hourly forecast for a city",
            description = """
                    Resolves the city to coordinates, then fetches the fine-grained hourly
                    forecast (up to ~156 hours) from the National Weather Service. \
                    Sibling to `GET /api/v1/weather` which uses 12-hour blocks.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Hourly forecast retrieved.",
                    content = @Content(schema = @Schema(implementation = HourlyWeatherResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "city is missing or blank.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404",
                    description = "City not found by Nominatim.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502",
                    description = "NWS unreachable or returned a non-success status.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<HourlyWeatherResponse> getHourlyWeather(
            @Parameter(description = "Free-text city name, e.g. `Arlington, VA`.",
                    example = "Arlington, VA", required = true)
            @RequestParam("city") @NotBlank String city) {
        WeatherQueryResult result = getWeather.execute(city);
        HourlyForecast hourly = getHourlyWeather.execute(city);
        return ResponseEntity.ok(toHourlyResponse(city, result, hourly));
    }

    @GetMapping("/metadata")
    @Operation(
            summary = "Get NWS Weather Forecast Office info for a city",
            description = """
                    Returns the WFO office id, human-readable name, timezone, radar \
                    station, and disclaimer URL for the resolved city. Useful for the \
                    "Forecast from NWS Baltimore/Washington · Sunrise 6:42, sunset 19:34" \
                    strip in the UI.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Office metadata retrieved.",
                    content = @Content(schema = @Schema(implementation = LocationMetadataResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "city is missing or blank.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404",
                    description = "City not found by Nominatim.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502",
                    description = "NWS unreachable or returned a non-success status.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LocationMetadataResponse> getMetadata(
            @Parameter(description = "Free-text city name, e.g. `Arlington, VA`.",
                    example = "Arlington, VA", required = true)
            @RequestParam("city") @NotBlank String city) {
        WeatherQueryResult result = getWeather.execute(city);
        io.ythalorossy.weatherapi.application.usecase.LocationMetadataResult meta =
                getLocationMetadata.execute(city);
        return ResponseEntity.ok(toMetadataResponse(city, result, meta));
    }

    @GetMapping("/forecast/discussion")
    @Operation(
            summary = "Get the latest Area Forecast Discussion (AFD) for a city's WFO",
            description = """
                    Resolves the city to its NWS Weather Forecast Office, then returns \
                    the latest Area Forecast Discussion text issued by that office. \
                    AFDs are issued several times per day and are cached server-side \
                    for 30 minutes.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Discussion retrieved.",
                    content = @Content(schema = @Schema(implementation = DiscussionResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "city is missing or blank.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404",
                    description = "City not found, no NWS coverage, or no AFD available.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502",
                    description = "NWS unreachable.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DiscussionResponse> getDiscussion(
            @Parameter(description = "Free-text city name, e.g. `Arlington, VA`.",
                    example = "Arlington, VA", required = true)
            @RequestParam("city") @NotBlank String city) {
        return getAfd.execute(city)
                .map(p -> ResponseEntity.ok(DiscussionResponse.from(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static WeatherResponse toResponse(String requestedCity, WeatherQueryResult result) {
        Location loc = result.location();
        WeatherForecast fc = result.forecast();
        return new WeatherResponse(
                requestedCity,
                new WeatherResponse.LocationView(loc.latitude(), loc.longitude(), loc.displayName()),
                new WeatherResponse.ForecastView(
                        fc.generatedAt(),
                        fc.source(),
                        fc.periods().stream().map(WeatherController::toPeriodView).toList()
                )
        );
    }

    private static HourlyWeatherResponse toHourlyResponse(String requestedCity,
                                                         WeatherQueryResult result,
                                                         HourlyForecast hourly) {
        Location loc = result.location();
        HourlyWeatherResponse.ForecastView forecastView = new HourlyWeatherResponse.ForecastView(
                hourly.generatedAt(),
                hourly.source(),
                hourly.periods().stream()
                        .map(WeatherController::toHourlyPeriodView)
                        .toList()
        );
        return new HourlyWeatherResponse(
                requestedCity,
                new HourlyWeatherResponse.LocationView(loc.latitude(), loc.longitude(), loc.displayName()),
                forecastView
        );
    }

    private static LocationMetadataResponse toMetadataResponse(String requestedCity,
                                                              WeatherQueryResult result,
                                                              io.ythalorossy.weatherapi.application.usecase.LocationMetadataResult meta) {
        Location loc = result.location();
        LocationMetadataResponse.SunView sunView = meta.sunTimes()
                .map(LocationMetadataResponse.SunView::from)
                .orElse(null);
        return new LocationMetadataResponse(
                requestedCity,
                new WeatherResponse.LocationView(loc.latitude(), loc.longitude(), loc.displayName()),
                LocationMetadataResponse.OfficeView.from(meta.office()),
                sunView
        );
    }

    private static WeatherResponse.PeriodView toPeriodView(ForecastPeriod p) {
        Temperature t = p.temperature();
        return new WeatherResponse.PeriodView(
                p.name(),
                new WeatherResponse.TemperatureView(t.value(), t.unit().name(), t.formatted()),
                p.windSpeed(),
                p.windDirection(),
                p.shortForecast(),
                p.detailedForecast(),
                p.daytime()
        );
    }

    private static HourlyWeatherResponse.PeriodView toHourlyPeriodView(io.ythalorossy.weatherapi.domain.model.HourlyForecastPeriod p) {
        Temperature t = p.temperature();
        return new HourlyWeatherResponse.PeriodView(
                p.startTime(),
                new HourlyWeatherResponse.TemperatureView(t.value(), t.unit().name(), t.formatted()),
                p.windSpeed(),
                p.windDirection(),
                p.shortForecast(),
                p.daytime()
        );
    }
}