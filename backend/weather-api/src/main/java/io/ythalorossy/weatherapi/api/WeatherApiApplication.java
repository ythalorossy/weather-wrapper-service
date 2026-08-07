package io.ythalorossy.weatherapi.api;

import io.ythalorossy.weatherapi.infrastructure.config.WeatherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot entry point.
 *
 * <p>Scans {@code io.ythalorossy.weatherapi.*} so adapters and use cases are picked up.
 * Enables {@link WeatherProperties} for {@code weather.*} YAML binding.
 */
@SpringBootApplication
@ComponentScan(basePackages = "io.ythalorossy.weatherapi")
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherApiApplication.class, args);
    }
}