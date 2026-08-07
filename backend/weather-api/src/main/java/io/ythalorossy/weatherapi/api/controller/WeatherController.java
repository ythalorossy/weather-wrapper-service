package io.ythalorossy.weatherapi.api.controller;

import io.ythalorossy.weatherapi.api.dto.WeatherResponse;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.application.usecase.WeatherQueryResult;
import io.ythalorossy.weatherapi.domain.model.ForecastPeriod;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Temperature;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@Validated
public class WeatherController {

    private final GetWeatherUseCase getWeather;

    public WeatherController(GetWeatherUseCase getWeather) {
        this.getWeather = getWeather;
    }

    /**
     * Get the current weather forecast for a city.
     *
     * @param city free-text city name (e.g., {@code "Arlington, VA"})
     * @return 200 with the forecast; 404 if the city can't be geocoded; 502 if the
     *         upstream weather provider fails; 400 if {@code city} is blank.
     */
    @GetMapping
    public ResponseEntity<WeatherResponse> getWeather(
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