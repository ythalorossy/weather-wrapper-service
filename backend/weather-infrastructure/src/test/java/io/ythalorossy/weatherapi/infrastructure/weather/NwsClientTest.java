package io.ythalorossy.weatherapi.infrastructure.weather;

import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NwsClientTest {

    private static final Location LOC = new Location(38.88, -77.09, "Arlington, VA");

    @org.junit.jupiter.api.Test
    void returnsValueOnSuccess() {
        Supplier<String> s = () -> "ok";
        assertThat(NwsClient.invoke(s, "NWS /foo", LOC)).isEqualTo("ok");
    }

    @org.junit.jupiter.api.Test
    void wraps4xxAsUnavailable() {
        Supplier<String> s = () -> { throw org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.NOT_FOUND, "nf",
                org.springframework.http.HttpHeaders.EMPTY, null, null); };
        assertThatThrownBy(() -> NwsClient.invoke(s, "NWS /foo", LOC))
            .isInstanceOf(WeatherProviderUnavailableException.class)
            .hasMessageContaining("NWS /foo")
            .hasMessageContaining("404")
            .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @org.junit.jupiter.api.Test
    void wraps5xxAsUnavailable() {
        Supplier<String> s = () -> { throw HttpServerErrorException.create(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "ise",
                org.springframework.http.HttpHeaders.EMPTY, null, null); };
        assertThatThrownBy(() -> NwsClient.invoke(s, "NWS /foo", LOC))
            .isInstanceOf(WeatherProviderUnavailableException.class)
            .hasMessageContaining("500");
    }

    @org.junit.jupiter.api.Test
    void wrapsOtherExceptionsAsUnavailable() {
        Supplier<String> s = () -> { throw new ResourceAccessException("connect refused"); };
        assertThatThrownBy(() -> NwsClient.invoke(s, "NWS /foo", LOC))
            .isInstanceOf(WeatherProviderUnavailableException.class)
            .hasMessageContaining("connect refused");
    }
}