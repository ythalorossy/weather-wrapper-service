package io.ythalorossy.weatherapi.infrastructure.weather;

import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.springframework.stereotype.Component;
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
        String key = "%.4f,%.4f".formatted(lat, lon);
        return memo.computeIfAbsent(key, k ->
                client.get().uri("/points/{lat},{lon}", lat, lon)
                        .retrieve().body(PointsResponse.class));
    }
}