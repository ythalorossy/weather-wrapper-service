package io.ythalorossy.weatherapi.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.ythalorossy.weatherapi.application.usecase.GetActiveAlertsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetAfdUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetCurrentConditionsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetHourlyForecastUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetLocationMetadataUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetSunTimesUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.application.usecase.LocationResolver;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.AfdCache;
import io.ythalorossy.weatherapi.domain.port.AlertCache;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;
import io.ythalorossy.weatherapi.domain.port.LocationCache;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;
import io.ythalorossy.weatherapi.domain.port.SunTimesCache;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
import io.ythalorossy.weatherapi.infrastructure.cache.RedisJsonCache;
import io.ythalorossy.weatherapi.infrastructure.config.WeatherProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wires the plain-Java use cases into the Spring container.
 *
 * <p>Lives in the API module because that's the composition root — adapters
 * are auto-discovered via {@code @Component}, but the use cases are framework-
 * agnostic and need explicit factory methods.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public LocationResolver locationResolver(
            GeocodingProvider geocodingProvider,
            LocationCache locationCache,
            WeatherProperties properties) {
        return new LocationResolver(
                geocodingProvider,
                locationCache,
                properties.getGeocoding().getCache().getTtl(),
                properties.getGeocoding().getCache().getAbsentTtl()
        );
    }

    @Bean
    public Cache<WeatherForecast> weatherCache(
            StringRedisTemplate redis,
            ObjectMapper mapper,
            MeterRegistry meters) {
        return new RedisJsonCache<>(redis, mapper, "weather", WeatherForecast.class, meters);
    }

    @Bean
    public Cache<HourlyForecast> hourlyForecastCache(
            StringRedisTemplate redis,
            ObjectMapper mapper,
            MeterRegistry meters) {
        return new RedisJsonCache<>(redis, mapper, "hourly", HourlyForecast.class, meters);
    }

    @Bean
    public Cache<Observation> observationCache(
            StringRedisTemplate redis,
            ObjectMapper mapper,
            MeterRegistry meters) {
        return new RedisJsonCache<>(redis, mapper, "obs", Observation.class, meters);
    }

    @Bean
    public GetWeatherUseCase getWeatherUseCase(
            WeatherProvider weatherProvider,
            Cache<WeatherForecast> weatherCache,
            LocationResolver locationResolver,
            WeatherProperties properties) {
        return new GetWeatherUseCase(
                weatherProvider,
                weatherCache,
                locationResolver,
                properties.getCache().getTtl()
        );
    }

    @Bean
    public GetHourlyForecastUseCase getHourlyForecastUseCase(
            HourlyWeatherProvider hourlyWeatherProvider,
            Cache<HourlyForecast> hourlyForecastCache,
            LocationResolver locationResolver,
            WeatherProperties properties) {
        // Hourly forecast uses the same 12 h cache TTL as daily; NWS publishes
        // both off the same gridpoint pipeline.
        return new GetHourlyForecastUseCase(
                hourlyWeatherProvider,
                hourlyForecastCache,
                locationResolver,
                properties.getCache().getTtl()
        );
    }

    @Bean
    public GetSunTimesUseCase getSunTimesUseCase(
            SunTimesProvider sunTimesProvider,
            SunTimesCache sunTimesCache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            WeatherProperties properties) {
        return new GetSunTimesUseCase(
                sunTimesProvider,
                sunTimesCache,
                locationResolver,
                metadataProvider,
                properties.getSun().getTtl()
        );
    }

    @Bean
    public GetLocationMetadataUseCase getLocationMetadataUseCase(
            LocationMetadataProvider metadataProvider,
            GetSunTimesUseCase getSunTimes,
            LocationResolver locationResolver) {
        return new GetLocationMetadataUseCase(metadataProvider, getSunTimes, locationResolver);
    }

    @Bean
    public GetCurrentConditionsUseCase getCurrentConditionsUseCase(
            ObservationProvider observationProvider,
            Cache<Observation> observationCache,
            LocationResolver locationResolver,
            WeatherProperties properties) {
        return new GetCurrentConditionsUseCase(
                observationProvider,
                observationCache,
                locationResolver,
                properties.getObservations().getTtl());
    }

    @Bean
    public GetActiveAlertsUseCase getActiveAlertsUseCase(
            AlertProvider alertProvider,
            AlertCache alertCache,
            LocationResolver locationResolver,
            WeatherProperties properties) {
        return new GetActiveAlertsUseCase(
                alertProvider,
                alertCache,
                locationResolver,
                properties.getObservations().getAlertTtl());
    }

    @Bean
    public GetAfdUseCase getAfdUseCase(
            AreaForecastDiscussionProvider afdProvider,
            AfdCache afdCache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            WeatherProperties properties) {
        return new GetAfdUseCase(
                afdProvider,
                afdCache,
                locationResolver,
                metadataProvider,
                properties.getAfd().getTtl());
    }
}