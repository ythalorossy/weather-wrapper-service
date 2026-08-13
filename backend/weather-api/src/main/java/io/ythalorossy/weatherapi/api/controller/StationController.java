package io.ythalorossy.weatherapi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.ythalorossy.weatherapi.application.usecase.GetStationObservationsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetStationsUseCase;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.model.StationObservation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather/stations")
@Validated
@Tag(name = "Stations", description = "NWS observation stations and their recent observations.")
public class StationController {

    private final GetStationsUseCase getStations;
    private final GetStationObservationsUseCase getStationObservations;

    public StationController(
            GetStationsUseCase getStations,
            GetStationObservationsUseCase getStationObservations) {
        this.getStations = getStations;
        this.getStationObservations = getStationObservations;
    }

    @GetMapping
    @Operation(
            summary = "Get nearby NWS observation stations for a city",
            description = """
                    Resolves the city to its NWS gridpoint, then returns the list of
                    observation stations whose coverage overlaps that gridpoint. 1-hour cache.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stations retrieved."),
            @ApiResponse(responseCode = "400", description = "city is missing or blank."),
            @ApiResponse(responseCode = "404", description = "City not found by Nominatim."),
            @ApiResponse(responseCode = "502", description = "NWS unreachable.")
    })
    public ResponseEntity<List<Station>> getStations(
            @Parameter(description = "Free-text city name, e.g. `Arlington, VA`.",
                    example = "Arlington, VA", required = true)
            @RequestParam("city") @NotBlank String city) {
        return ResponseEntity.ok(getStations.execute(city));
    }

    @GetMapping("/{stationId}/observations")
    @Operation(
            summary = "Get recent observations for an NWS station",
            description = """
                    Returns the recent observations reported by a single NWS station,
                    newest first. 10-minute cache keyed by station id.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Observations retrieved."),
            @ApiResponse(responseCode = "400", description = "stationId is missing or blank."),
            @ApiResponse(responseCode = "502", description = "NWS unreachable.")
    })
    public ResponseEntity<List<StationObservation>> getObservations(
            @Parameter(description = "NWS station id, e.g. `KDCA`.",
                    example = "KDCA", required = true)
            @PathVariable("stationId") @NotBlank String stationId) {
        return ResponseEntity.ok(getStationObservations.execute(stationId));
    }
}
