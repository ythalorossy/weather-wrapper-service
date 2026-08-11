package io.ythalorossy.weatherapi.infrastructure.weather;

import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NwsPointsService {

    private final RestClient client;
    // # ponytail: per-process memoization, single-instance only.
    // For multi-instance deploy, share via Cache<PointsResponse> with prefix "points".
    private final Map<String, PointsResponse> memo = new ConcurrentHashMap<>();

    public NwsPointsService(RestClient nwsRestClient) {
        this.client = nwsRestClient;
    }

    public PointsResponse lookup(double lat, double lon) {
        return lookup(new Location(lat, lon, "%.4f,%.4f".formatted(lat, lon)));
    }

    public PointsResponse lookup(Location location) {
        String key = "%.4f,%.4f".formatted(location.latitude(), location.longitude());
        return memo.computeIfAbsent(key, k -> {
            try {
                return client.get().uri("/points/{lat},{lon}", location.latitude(), location.longitude())
                        .retrieve().body(PointsResponse.class);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new WeatherProviderUnavailableException(
                        "NWS /points returned " + e.getStatusCode() + " for " + location.displayName(), e);
            } catch (Exception e) {
                throw new WeatherProviderUnavailableException(
                        "Failed to call NWS /points for " + location.displayName(), e);
            }
        });
    }

    void clearMemo() {
        memo.clear();
    }
}