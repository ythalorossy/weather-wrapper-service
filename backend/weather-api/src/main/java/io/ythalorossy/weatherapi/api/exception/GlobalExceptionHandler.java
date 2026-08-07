package io.ythalorossy.weatherapi.api.exception;

import io.ythalorossy.weatherapi.domain.exception.LocationNotFoundException;
import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Maps domain and request-validation exceptions to HTTP status codes using
 * Spring's {@link ProblemDetail} (RFC 9457).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationNotFoundException.class)
    public ProblemDetail handleLocationNotFound(LocationNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setType(URI.create("https://weather-wrapper-service.ythalorossy.io/errors/location-not-found"));
        pd.setTitle("Location not found");
        pd.setProperty("city", e.cityName());
        return pd;
    }

    @ExceptionHandler(WeatherProviderUnavailableException.class)
    public ProblemDetail handleWeatherProviderUnavailable(WeatherProviderUnavailableException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        pd.setType(URI.create("https://weather-wrapper-service.ythalorossy.io/errors/upstream-unavailable"));
        pd.setTitle("Upstream weather provider unavailable");
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setType(URI.create("https://weather-wrapper-service.ythalorossy.io/errors/invalid-request"));
        pd.setTitle("Invalid request");
        return pd;
    }

    /**
     * Bean Validation failures on @RequestParam / @PathVariable (e.g., @NotBlank).
     * Maps to 400 with the list of violations as a {@code violations} property.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        String violations = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed: " + violations);
        pd.setType(URI.create("https://weather-wrapper-service.ythalorossy.io/errors/validation"));
        pd.setTitle("Invalid request");
        return pd;
    }
}