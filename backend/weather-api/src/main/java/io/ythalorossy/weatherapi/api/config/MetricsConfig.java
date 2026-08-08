package io.ythalorossy.weatherapi.api.config;

import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common tags shared by every metric emitted by this service.
 *
 * <p>Adding {@code service=weather-wrapper-service} lets dashboards group/split
 * metrics when this app is deployed alongside other services sharing a
 * Prometheus scrape target.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder commonTagsBinder() {
        return registry -> registry.config().commonTags("service", "weather-wrapper-service");
    }
}