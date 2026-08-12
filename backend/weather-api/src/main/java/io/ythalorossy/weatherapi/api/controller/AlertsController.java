package io.ythalorossy.weatherapi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.ythalorossy.weatherapi.api.dto.AlertsResponse;
import io.ythalorossy.weatherapi.api.dto.WeatherResponse;
import io.ythalorossy.weatherapi.application.usecase.GetActiveAlertsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Active weather alerts at a city. Returns 200 with an empty list when no
 * alerts are active (not 404 \u2014 absence of alerts is normal).
 *
 * <p>5-minute cache (alerts change fast).
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Validated
@Tag(name = "Alerts", description = "Active NWS weather alerts at a city.")
public class AlertsController {

    private final GetActiveAlertsUseCase getActiveAlerts;
    private final GetWeatherUseCase getWeather;

    public AlertsController(
            GetActiveAlertsUseCase getActiveAlerts,
            GetWeatherUseCase getWeather) {
        this.getActiveAlerts = getActiveAlerts;
        this.getWeather = getWeather;
    }

    @GetMapping
    @Operation(
            summary = "Get active NWS weather alerts at a city",
            description = """
                    Resolves the city to coordinates and returns the active alerts from
                    NWS /alerts/active. Empty list means no alerts are active. 5-minute cache.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Alerts list (possibly empty).",
                    content = @Content(schema = @Schema(implementation = AlertsResponse.class))),
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
    public ResponseEntity<AlertsResponse> getAlerts(
            @Parameter(description = "Free-text city name, e.g. `Arlington, VA`.",
                    example = "Arlington, VA", required = true)
            @RequestParam("city") @NotBlank String city) {
        Location location = getWeather.execute(city).location();
        List<WeatherAlert> alerts = getActiveAlerts.execute(city);

        List<AlertsResponse.AlertView> views = alerts.stream()
                .map(AlertsResponse.AlertView::from)
                .toList();

        return ResponseEntity.ok(new AlertsResponse(
                city,
                new WeatherResponse.LocationView(location.latitude(), location.longitude(), location.displayName()),
                views
        ));
    }
}