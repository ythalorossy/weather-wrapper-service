package io.ythalorossy.weatherapi.infrastructure.weather;

import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.function.Supplier;

public final class NwsClient {

    private NwsClient() {}

    public static <T> T invoke(Supplier<T> call, String op, Location location) {
        try {
            return call.get();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new WeatherProviderUnavailableException(
                    op + " returned " + e.getStatusCode() + " for " + location.displayName(), e);
        } catch (Exception e) {
            throw new WeatherProviderUnavailableException(
                    "Failed to call " + op + " for " + location.displayName() + ": " + e.getMessage(), e);
        }
    }
}