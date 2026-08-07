package io.ythalorossy.weatherapi.api.config;

import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.WeatherCache;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
import io.ythalorossy.weatherapi.infrastructure.config.WeatherProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the plain-Java {@link GetWeatherUseCase} into the Spring container.
 *
 * <p>Lives in the API module because that's the composition root — adapters
 * are auto-discovered via {@code @Component}, but the use case is framework-
 * agnostic and needs an explicit factory method.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public GetWeatherUseCase getWeatherUseCase(
            GeocodingProvider geocodingProvider,
            WeatherProvider weatherProvider,
            WeatherCache weatherCache,
            WeatherProperties properties) {
        return new GetWeatherUseCase(
                geocodingProvider,
                weatherProvider,
                weatherCache,
                properties.getCache().getTtl()
        );
    }
}