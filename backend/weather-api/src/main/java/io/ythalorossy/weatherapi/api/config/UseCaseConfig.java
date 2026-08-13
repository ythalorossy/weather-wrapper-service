package io.ythalorossy.weatherapi.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.ythalorossy.weatherapi.application.usecase.GetActiveAlertsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetAfdUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetCurrentConditionsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetHourlyForecastUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetLocationMetadataUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetStationObservationsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetStationsUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetSunTimesUseCase;
import io.ythalorossy.weatherapi.application.usecase.GetWeatherUseCase;
import io.ythalorossy.weatherapi.application.usecase.LocationResolver;
import io.ythalorossy.weatherapi.domain.model.AfdProduct;
import io.ythalorossy.weatherapi.domain.model.HourlyForecast;
import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.Observation;
import io.ythalorossy.weatherapi.domain.model.Station;
import io.ythalorossy.weatherapi.domain.model.StationObservation;
import io.ythalorossy.weatherapi.domain.model.SunTimes;
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;
import io.ythalorossy.weatherapi.domain.port.AlertProvider;
import io.ythalorossy.weatherapi.domain.port.AreaForecastDiscussionProvider;
import io.ythalorossy.weatherapi.domain.port.Cache;
import io.ythalorossy.weatherapi.domain.port.GeocodingProvider;
import io.ythalorossy.weatherapi.domain.port.HourlyWeatherProvider;
import io.ythalorossy.weatherapi.domain.port.LocationMetadataProvider;
import io.ythalorossy.weatherapi.domain.port.ObservationProvider;
import io.ythalorossy.weatherapi.domain.port.PointsProvider;
import io.ythalorossy.weatherapi.domain.port.StationObservationProvider;
import io.ythalorossy.weatherapi.domain.port.StationsProvider;
import io.ythalorossy.weatherapi.domain.port.SunTimesProvider;
import io.ythalorossy.weatherapi.domain.port.WeatherProvider;
import io.ythalorossy.weatherapi.infrastructure.cache.RedisJsonCache;
import io.ythalorossy.weatherapi.infrastructure.config.WeatherProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

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
    public Cache<Location> locationCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("geo", Location.class, redis, mapper, meters);
    }

    @Bean
    public LocationResolver locationResolver(
            GeocodingProvider geocodingProvider,
            Cache<Location> locationCache,
            WeatherProperties properties) {
        return new LocationResolver(
                geocodingProvider,
                locationCache,
                properties.getGeocodingCacheTtl(),
                properties.getGeocodingAbsentTtl()
        );
    }

    @Bean
    public Cache<WeatherForecast> weatherCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("weather", WeatherForecast.class, redis, mapper, meters);
    }

    @Bean
    public Cache<HourlyForecast> hourlyForecastCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("hourly", HourlyForecast.class, redis, mapper, meters);
    }

    @Bean
    public Cache<Observation> observationCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("obs", Observation.class, redis, mapper, meters);
    }

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Cache<List<WeatherAlert>> alertsCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("alerts", (Class<List<WeatherAlert>>) (Class) List.class, redis, mapper, meters);
    }

    @Bean
    public Cache<AfdProduct> afdCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("afd", AfdProduct.class, redis, mapper, meters);
    }

    @Bean
    public Cache<SunTimes> sunTimesCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("sun", SunTimes.class, redis, mapper, meters);
    }

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Cache<List<Station>> stationsCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("stations", (Class<List<Station>>) (Class) List.class, redis, mapper, meters);
    }

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Cache<List<StationObservation>> stationObservationsCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("station-obs", (Class<List<StationObservation>>) (Class) List.class, redis, mapper, meters);
    }

    private static <V> Cache<V> cache(String prefix, Class<V> type,
                                      StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return new RedisJsonCache<>(redis, mapper, prefix, type, meters);
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
                properties.getCacheTtl()
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
                properties.getCacheTtl()
        );
    }

    @Bean
    public GetSunTimesUseCase getSunTimesUseCase(
            SunTimesProvider sunTimesProvider,
            Cache<SunTimes> sunTimesCache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            WeatherProperties properties) {
        return new GetSunTimesUseCase(
                sunTimesProvider,
                sunTimesCache,
                locationResolver,
                metadataProvider,
                properties.getSunTtl()
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
                properties.getObservationTtl());
    }

    @Bean
    public GetActiveAlertsUseCase getActiveAlertsUseCase(
            AlertProvider alertProvider,
            Cache<List<WeatherAlert>> alertsCache,
            LocationResolver locationResolver,
            WeatherProperties properties) {
        return new GetActiveAlertsUseCase(
                alertProvider,
                alertsCache,
                locationResolver,
                properties.getAlertTtl());
    }

    @Bean
    public GetAfdUseCase getAfdUseCase(
            AreaForecastDiscussionProvider afdProvider,
            Cache<AfdProduct> afdCache,
            LocationResolver locationResolver,
            LocationMetadataProvider metadataProvider,
            WeatherProperties properties) {
        return new GetAfdUseCase(
                afdProvider,
                afdCache,
                locationResolver,
                metadataProvider,
                properties.getAfdTtl());
    }

    @Bean
    public GetStationsUseCase getStationsUseCase(
            LocationResolver locationResolver,
            StationsProvider stationsProvider,
            PointsProvider pointsProvider,
            Cache<List<Station>> stationsCache) {
        return new GetStationsUseCase(
                locationResolver,
                stationsProvider,
                pointsProvider,
                stationsCache);
    }

    @Bean
    public GetStationObservationsUseCase getStationObservationsUseCase(
            StationObservationProvider observationProvider,
            Cache<List<StationObservation>> stationObservationsCache) {
        // ponytail: TTL hardcoded here. Move to WeatherProperties + application.yml
        // (e.g. station-observations-ttl) when ops needs to tune it.
        return new GetStationObservationsUseCase(
                observationProvider,
                stationObservationsCache,
                java.time.Duration.ofMinutes(10));
    }
}