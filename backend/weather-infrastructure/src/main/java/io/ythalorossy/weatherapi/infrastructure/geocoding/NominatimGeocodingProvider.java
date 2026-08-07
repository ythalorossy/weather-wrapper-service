package io.ythalorossy.weatherapi.infrastructure.geocoding;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;
import java.util.Optional;

/**
 * Nominatim adapter for {@link GeocodingProvider}.
 *
 * <p>Calls {@code https://nominatim.openstreetmap.org/search?q=...&format=json&limit=1}
 * and returns the first match as a {@link Location}.
 */
@Component
public class NominatimGeocodingProvider implements GeocodingProvider {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingProvider.class);

    private final RestClient client;

    public NominatimGeocodingProvider(RestClient nominatimRestClient) {
        this.client = nominatimRestClient;
    }

    @Override
    public Optional<Location> findLocation(String cityName) {
        Objects.requireNonNull(cityName, "cityName");
        if (cityName.isBlank()) {
            throw new IllegalArgumentException("cityName must not be blank");
        }

        log.debug("Geocoding city via Nominatim: {}", cityName);

        NominatimResult[] results = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", cityName)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .queryParam("addressdetails", 0)
                        .build())
                .retrieve()
                .body(NominatimResult[].class);

        if (results == null || results.length == 0) {
            log.debug("No geocoding results for: {}", cityName);
            return Optional.empty();
        }

        NominatimResult first = results[0];
        try {
            double lat = Double.parseDouble(first.lat());
            double lon = Double.parseDouble(first.lon());
            String displayName = first.displayName() != null ? first.displayName() : cityName;
            return Optional.of(new Location(lat, lon, displayName));
        } catch (NumberFormatException e) {
            log.warn("Invalid lat/lon in Nominatim response for {}: lat={}, lon={}",
                    cityName, first.lat(), first.lon());
            return Optional.empty();
        }
    }
}