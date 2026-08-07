package io.ythalorossy.weatherapi.infrastructure;

import io.ythalorossy.weatherapi.infrastructure.config.WeatherProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot config for infrastructure tests.
 *
 * <p>Lives next to the tests so {@code weather-infrastructure} can run
 * {@code @SpringBootTest} without depending on {@code weather-api}'s
 * {@code WeatherApiApplication}.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "io.ythalorossy.weatherapi.infrastructure")
@EnableConfigurationProperties(WeatherProperties.class)
public class InfrastructureTestConfig {
}