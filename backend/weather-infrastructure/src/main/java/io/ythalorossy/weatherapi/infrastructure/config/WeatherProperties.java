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
    private Observations observations = new Observations();
    private Provider provider = new Provider();
    private Geocoding geocoding = new Geocoding();
    private Sun sun = new Sun();

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public Observations getObservations() { return observations; }
    public void setObservations(Observations observations) { this.observations = observations; }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public Geocoding getGeocoding() { return geocoding; }
    public void setGeocoding(Geocoding geocoding) { this.geocoding = geocoding; }

    public Sun getSun() { return sun; }
    public void setSun(Sun sun) { this.sun = sun; }

    public static class Cache {
        private Duration ttl = Duration.ofHours(12);
        private Duration absentTtl;

        public Cache() {
            // default 12h, used for weather.cache.ttl
        }

        public Cache(Duration defaultTtl) {
            // explicit default for nested caches (e.g., geocoding.cache.ttl)
            this.ttl = defaultTtl;
        }

        public Cache(Duration defaultTtl, Duration defaultAbsentTtl) {
            // for caches that support negative caching (e.g., geocoding)
            this.ttl = defaultTtl;
            this.absentTtl = defaultAbsentTtl;
        }

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }

        public Duration getAbsentTtl() { return absentTtl; }
        public void setAbsentTtl(Duration absentTtl) { this.absentTtl = absentTtl; }
    }

    public static class Observations {
        /** Cache TTL for current-conditions observations (live station data). */
        private Duration ttl = Duration.ofMinutes(10);
        /** Cache TTL for active weather alerts (short — alerts change fast). */
        private Duration alertTtl = Duration.ofMinutes(5);

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }

        public Duration getAlertTtl() { return alertTtl; }
        public void setAlertTtl(Duration alertTtl) { this.alertTtl = alertTtl; }
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
        private Cache cache = new Cache(Duration.ofDays(30), Duration.ofSeconds(60));

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public Cache getCache() { return cache; }
        public void setCache(Cache cache) { this.cache = cache; }
    }

    public static class Sun {
        /** Cache TTL for sunrise/sunset data; date-keyed so 48h covers a day rollover. */
        private Duration ttl = Duration.ofHours(48);

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }
}