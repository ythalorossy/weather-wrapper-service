package io.ythalorossy.weatherapi.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient beans for upstream HTTP APIs.
 *
 * <p>One bean per upstream service so headers, base URL, and timeouts are
 * configured independently. Both clients set a descriptive {@code User-Agent}
 * (required by NWS, requested by Nominatim).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nwsRestClient(WeatherProperties props) {
        var p = props.getProvider();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) p.getTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(p.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, p.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestClient nominatimRestClient(WeatherProperties props) {
        var g = props.getGeocoding();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) g.getTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(g.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, g.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}