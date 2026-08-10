package io.ythalorossy.weatherapi.infrastructure.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * NWS adapter for {@link AreaForecastDiscussionProvider}.
 *
 * <p>Two-step: {@code GET /products/types/AFD} (list of recent AFDs) →
 * filter by {@code issuingOffice}, pick the most recent by
 * {@code issuanceTime} → {@code GET /products/{id}} (full product body).
 *
 * <p>All upstream failures are absorbed into {@code Optional.empty()} so
 * the application layer treats "no AFD" and "NWS down" identically.
 */
@Component
public class NwsAfdProvider implements AreaForecastDiscussionProvider {

    private static final Logger log = LoggerFactory.getLogger(NwsAfdProvider.class);
    private static final String TIMER_NAME = "weather.provider.afd";

    private final RestClient client;
    private final MeterRegistry meterRegistry;

    public NwsAfdProvider(RestClient nwsRestClient, MeterRegistry meterRegistry) {
        this.client = nwsRestClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<AfdProduct> getLatest(String officeId) {
        requireOfficeId(officeId);

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Optional<AfdProduct> result = doFetch(officeId);
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "success"));
            return result;
        } catch (Exception e) {
            log.warn("AFD fetch failed for {}: {}", officeId, e.getMessage());
            sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "failure"));
            return Optional.empty();
        }
    }

    private Optional<AfdProduct> doFetch(String officeId) {
        AfdListResponse list = client.get()
                .uri("/products/types/AFD")
                .retrieve()
                .body(AfdListResponse.class);

        List<AfdListEntry> matching = list == null || list.graph() == null
                ? List.of()
                : list.graph().stream()
                        .filter(e -> officeId.equals(e.issuingOffice()))
                        .toList();

        if (matching.isEmpty()) {
            return Optional.empty();
        }

        AfdListEntry latest = matching.stream()
                .max((a, b) -> a.issuanceTime().compareTo(b.issuanceTime()))
                .orElseThrow();

        // id is a full URL like "https://api.weather.gov/products/{id}";
        // we need the bare tail.
        String tail = tailAfterLastSlash(latest.id());
        if (tail.isEmpty()) return Optional.empty();

        AfdProductResponse product = client.get()
                .uri("/products/{id}", tail)
                .retrieve()
                .body(AfdProductResponse.class);

        if (product == null || product.productText() == null) {
            return Optional.empty();
        }

        return Optional.of(new AfdProduct(officeId, latest.issuanceTime(), product.productText()));
    }

    private static String tailAfterLastSlash(String s) {
        if (s == null) return "";
        int idx = s.lastIndexOf('/');
        return idx < 0 ? s : s.substring(idx + 1);
    }

    // ----- JSON DTOs (no shared module — single-use shapes) -----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AfdListResponse(@JsonProperty("@graph") List<AfdListEntry> graph) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AfdListEntry(
            String id,
            String issuingOffice,
            Instant issuanceTime) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AfdProductResponse(
            String id,
            String issuingOffice,
            Instant issuanceTime,
            @JsonProperty("productText") String productText) {}
}