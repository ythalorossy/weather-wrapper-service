package io.ythalorossy.weatherapi.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient beans for upstream HTTP APIs.
 *
 * <p>One bean per upstream service so headers, base URL, and timeouts are
 * configured independently. Both clients set a descriptive {@code User-Agent}
 * (required by NWS, requested by Nominatim) and use the Apache HttpClient
 * request factory so they follow 3xx redirects (NWS returns 301 to canonicalize
 * coordinate precision; the JDK HttpURLConnection default does NOT follow).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nwsRestClient(WeatherProperties props) {
        var p = props.getProvider();
        return build(p.getBaseUrl(), p.getUserAgent());
    }

    @Bean
    public RestClient nominatimRestClient(WeatherProperties props) {
        var g = props.getGeocoding();
        return build(g.getBaseUrl(), g.getUserAgent());
    }

    private RestClient build(String baseUrl, String userAgent) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}