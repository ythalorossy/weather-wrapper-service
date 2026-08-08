package io.ythalorossy.weatherapi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.ythalorossy.weatherapi.api.dto.WeatherResponse;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.application.usecase.WeatherQueryResult;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
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

    public WeatherController(GetWeatherUseCase getWeather) {
        this.getWeather = getWeather;
    }

    @GetMapping
    @Operation(
            summary = "Get forecast for a city",
            description = """
                    Resolves the city to coordinates via Nominatim, then fetches the \
                    multi-period forecast from the National Weather Service. Results are \
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
}