package io.ythalorossy.weatherapi.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.ythalorossy.weatherapi.infrastructure.config.WeatherProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot config for infrastructure tests.
 *
 * <p>Lives next to the tests so {@code weather-infrastructure} can run
 * {@code @SpringBootTest} without depending on {@code weather-api}'s
 * {@code WeatherApiApplication}.
 *
 * <p>Provides a no-op {@link SimpleMeterRegistry} so the cache + provider
 * adapters can resolve their {@link MeterRegistry} dependency. The Prometheus
 * registry is only needed in the runtime {@code weather-api} module.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "io.ythalorossy.weatherapi.infrastructure")
@EnableConfigurationProperties(WeatherProperties.class)
public class InfrastructureTestConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}