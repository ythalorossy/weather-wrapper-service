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

    private static final URI LOCATION_NOT_FOUND_TYPE =
            URI.create("https://weather-wrapper-service.ythalorossy.io/errors/location-not-found");
    private static final URI UPSTREAM_UNAVAILABLE_TYPE =
            URI.create("https://weather-wrapper-service.ythalorossy.io/errors/upstream-unavailable");
    private static final URI INVALID_REQUEST_TYPE =
            URI.create("https://weather-wrapper-service.ythalorossy.io/errors/invalid-request");
    private static final URI VALIDATION_TYPE =
            URI.create("https://weather-wrapper-service.ythalorossy.io/errors/validation");

    @SuppressWarnings("null")
@ExceptionHandler(LocationNotFoundException.class)
    public ProblemDetail handleLocationNotFound(LocationNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setType(LOCATION_NOT_FOUND_TYPE);
        pd.setTitle("Location not found");
        pd.setProperty("city", e.cityName());
        return pd;
    }

    @SuppressWarnings("null")
@ExceptionHandler(WeatherProviderUnavailableException.class)
    public ProblemDetail handleWeatherProviderUnavailable(WeatherProviderUnavailableException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        pd.setType(UPSTREAM_UNAVAILABLE_TYPE);
        pd.setTitle("Upstream weather provider unavailable");
        return pd;
    }

    @SuppressWarnings("null")
@ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setType(INVALID_REQUEST_TYPE);
        pd.setTitle("Invalid request");
        return pd;
    }

    /**
     * Bean Validation failures on @RequestParam / @PathVariable (e.g., @NotBlank).
     * Maps to 400 with the list of violations as a {@code violations} property.
     */
    @SuppressWarnings("null")
@ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        String violations = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed: " + violations);
        pd.setType(VALIDATION_TYPE);
        pd.setTitle("Invalid request");
        return pd;
    }
}
