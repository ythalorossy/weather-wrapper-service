package io.ythalorossy.weatherapi.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound to the {@code weather.*} tree in {@code application.yml}.
 *
 * <p>Default values are baked in so the application starts even if no
 * configuration is provided. Anything in YAML overrides the defaults.
 */
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {

    private Cache cache = new Cache();
    private Provider provider = new Provider();
    private Geocoding geocoding = new Geocoding();

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public Geocoding getGeocoding() { return geocoding; }
    public void setGeocoding(Geocoding geocoding) { this.geocoding = geocoding; }

    public static class Cache {
        private Duration ttl = Duration.ofHours(12);

        public Cache() {
            // default 12h, used for weather.cache.ttl
        }

        public Cache(Duration defaultTtl) {
            // explicit default for nested caches (e.g., geocoding.cache.ttl)
            this.ttl = defaultTtl;
        }

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Provider {
        private String baseUrl = "https://api.weather.gov";
        private String userAgent = "weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)";
        private Duration timeout = Duration.ofSeconds(10);

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }

    public static class Geocoding {
        private String baseUrl = "https://nominatim.openstreetmap.org";
        private String userAgent = "weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)";
        private Duration timeout = Duration.ofSeconds(5);
        private Cache cache = new Cache(Duration.ofDays(30));

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public Cache getCache() { return cache; }
        public void setCache(Cache cache) { this.cache = cache; }
    }
}