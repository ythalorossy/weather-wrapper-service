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

    private Duration cacheTtl = Duration.ofHours(12);
    private Duration geocodingCacheTtl = Duration.ofDays(30);
    private Duration geocodingAbsentTtl = Duration.ofSeconds(60);
    private Duration observationTtl = Duration.ofMinutes(10);
    private Duration alertTtl = Duration.ofMinutes(5);
    private String providerBaseUrl = "https://api.weather.gov";
    private String providerUserAgent = "weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)";
    private Duration providerTimeout = Duration.ofSeconds(10);
    private String geocodingBaseUrl = "https://nominatim.openstreetmap.org";
    private String geocodingUserAgent = "weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)";
    private Duration geocodingTimeout = Duration.ofSeconds(5);
    private Duration sunTtl = Duration.ofHours(48);
    private Duration afdTtl = Duration.ofMinutes(30);

    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
    public Duration getGeocodingCacheTtl() { return geocodingCacheTtl; }
    public void setGeocodingCacheTtl(Duration geocodingCacheTtl) { this.geocodingCacheTtl = geocodingCacheTtl; }
    public Duration getGeocodingAbsentTtl() { return geocodingAbsentTtl; }
    public void setGeocodingAbsentTtl(Duration geocodingAbsentTtl) { this.geocodingAbsentTtl = geocodingAbsentTtl; }
    public Duration getObservationTtl() { return observationTtl; }
    public void setObservationTtl(Duration observationTtl) { this.observationTtl = observationTtl; }
    public Duration getAlertTtl() { return alertTtl; }
    public void setAlertTtl(Duration alertTtl) { this.alertTtl = alertTtl; }
    public String getProviderBaseUrl() { return providerBaseUrl; }
    public void setProviderBaseUrl(String providerBaseUrl) { this.providerBaseUrl = providerBaseUrl; }
    public String getProviderUserAgent() { return providerUserAgent; }
    public void setProviderUserAgent(String providerUserAgent) { this.providerUserAgent = providerUserAgent; }
    public Duration getProviderTimeout() { return providerTimeout; }
    public void setProviderTimeout(Duration providerTimeout) { this.providerTimeout = providerTimeout; }
    public String getGeocodingBaseUrl() { return geocodingBaseUrl; }
    public void setGeocodingBaseUrl(String geocodingBaseUrl) { this.geocodingBaseUrl = geocodingBaseUrl; }
    public String getGeocodingUserAgent() { return geocodingUserAgent; }
    public void setGeocodingUserAgent(String geocodingUserAgent) { this.geocodingUserAgent = geocodingUserAgent; }
    public Duration getGeocodingTimeout() { return geocodingTimeout; }
    public void setGeocodingTimeout(Duration geocodingTimeout) { this.geocodingTimeout = geocodingTimeout; }
    public Duration getSunTtl() { return sunTtl; }
    public void setSunTtl(Duration sunTtl) { this.sunTtl = sunTtl; }
    public Duration getAfdTtl() { return afdTtl; }
    public void setAfdTtl(Duration afdTtl) { this.afdTtl = afdTtl; }
}
