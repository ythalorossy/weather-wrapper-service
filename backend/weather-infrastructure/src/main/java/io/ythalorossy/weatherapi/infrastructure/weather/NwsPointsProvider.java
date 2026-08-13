package io.ythalorossy.weatherapi.infrastructure.weather;

import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
import io.ythalorossy.weatherapi.domain.model.Gridpoint;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.port.PointsProvider;
import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * NWS adapter for {@link PointsProvider}.
 *
 * <p>Delegates the {@code /points/{lat},{lon}} HTTP call to the shared
 * {@link NwsPointsService}, which memoizes results per-coordinate within a
 * single process. Other adapters (e.g. {@link NwsLocationMetadataProvider})
 * route through the same service, so memoization is shared across the app.
 */
@Component
public class NwsPointsProvider implements PointsProvider {

    private final NwsPointsService pointsService;

    public NwsPointsProvider(NwsPointsService pointsService) {
        this.pointsService = Objects.requireNonNull(pointsService, "pointsService");
    }

    @Override
    public Gridpoint getGridpoint(Location location) {
        Objects.requireNonNull(location, "location");
        PointsResponse points = pointsService.lookup(location);

        if (points == null || points.properties() == null) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned empty body for " + location.displayName());
        }

        String gridId = points.properties().gridId();
        if (gridId == null || gridId.isBlank()) {
            throw new WeatherProviderUnavailableException(
                    "NWS /points returned no gridId for " + location.displayName());
        }

        return new Gridpoint(gridId, points.properties().gridX(), points.properties().gridY());
    }
}
