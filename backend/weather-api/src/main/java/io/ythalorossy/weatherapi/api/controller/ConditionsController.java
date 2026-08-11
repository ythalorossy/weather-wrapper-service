package io.ythalorossy.weatherapi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.ythalorossy.weatherapi.api.dto.CurrentConditionsResponse;
import io.ythalorossy.weatherapi.api.dto.WeatherResponse;
import io.ythalorossy.weatherapi.application.usecase.GetCurrentConditionsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.application.usecase.WeatherQueryResult;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current conditions at the nearest NWS observation station.
 *
 * <p>Separate controller from {@link WeatherController} because the resource
 * (live conditions vs forecast) has a different TTL, a different upstream
 * endpoint, and a different cache shape. RESTful design favors distinct
 * resources over nested paths.
 */
@RestController
@RequestMapping("/api/v1/conditions")
@Validated
@Tag(name = "Conditions", description = "Current weather observations from nearby NWS stations.")
public class ConditionsController {

    private final GetCurrentConditionsUseCase getCurrentConditions;
    private final GetWeatherUseCase getWeather;

    public ConditionsController(
            GetCurrentConditionsUseCase getCurrentConditions,
            GetWeatherUseCase getWeather) {
        this.getCurrentConditions = getCurrentConditions;
        this.getWeather = getWeather;
    }

    @GetMapping
    @Operation(
            summary = "Get current conditions at the nearest NWS station",
            description = """
                    Resolves the city to coordinates, finds the nearest NWS observation
                    station, and returns its latest reported conditions (temp, dewpoint,
                    wind, humidity, pressure, free-text description). 10-minute cache.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Conditions retrieved.",
                    content = @Content(schema = @Schema(implementation = CurrentConditionsResponse.class))),
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
    public ResponseEntity<CurrentConditionsResponse> getConditions(
            @Parameter(description = "Free-text city name, e.g. `Arlington, VA`.",
                    example = "Arlington, VA", required = true)
            @RequestParam("city") @NotBlank String city) {
        // Reuse the resolver-backed daily use case just for the resolved Location.
        WeatherQueryResult geo = getWeather.execute(city);
        Location location = geo.location();

        Observation obs;
        try {
            obs = getCurrentConditions.execute(city);
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(new CurrentConditionsResponse(
                    city,
                    new WeatherResponse.LocationView(location.latitude(), location.longitude(), location.displayName()),
                    null
            ));
        }

        return ResponseEntity.ok(new CurrentConditionsResponse(
                city,
                new WeatherResponse.LocationView(location.latitude(), location.longitude(), location.displayName()),
                CurrentConditionsResponse.ObservationView.from(obs)
        ));
    }
}