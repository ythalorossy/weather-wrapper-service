package io.ythalorossy.weatherapi.domain.port;

import io.ythalorossy.weatherapi.domain.model.AfdProduct;

import java.util.Optional;

/**
 * Provider of the latest Area Forecast Discussion (AFD) issued by a WFO.
 *
 * <p>Implementations are expected to be side-effect free apart from the
 * upstream call; the application layer is responsible for caching.
 */
public interface AreaForecastDiscussionProvider {

    /**
     * @param officeId three-letter WFO identifier (e.g. {@code KLWX}); must not be null/blank
     * @return the latest AFD for the office, or empty if the upstream is
     *         unreachable, the office has no AFD, or the body fetch failed.
     */
    Optional<AfdProduct> getLatest(String officeId);
}