# Observability

Spring Boot Actuator exposes the Prometheus scrape endpoint at:

```
GET /actuator/prometheus    →  200, Prometheus exposition format
```

## Auto-collected metrics

Spring Boot + Micrometer + Lettuce give us a baseline for free:

| Metric prefix | What it covers |
|---|---|
| `http_server_requests_seconds` | Per-endpoint latency, count, status (auto from Spring MVC) |
| `jvm_memory_*`, `jvm_gc_*`, `jvm_threads_*` | JVM internals |
| `process_cpu_*`, `system_cpu_*` | CPU usage |
| `lettuce_command_*` | Redis client commands (auto, since Spring Boot uses Lettuce) |
| `logback_events_*` | Log counts by level |

## Custom metrics (weather_*)

Registered in the cache adapter (`RedisJsonCache<V>`) and upstream providers:

| Metric | Type | Tags | Where |
|---|---|---|---|
| `weather.cache.geo.hits` / `weather.cache.geo.misses` | Counter | — | `RedisJsonCache<Location>` |
| `weather.cache.weather.hits` / `weather.cache.weather.misses` | Counter | — | `RedisJsonCache<WeatherForecast>` |
| `weather.cache.hourly.hits` / `weather.cache.hourly.misses` | Counter | — | `RedisJsonCache<HourlyForecast>` |
| `weather.cache.obs.hits` / `weather.cache.obs.misses` | Counter | — | `RedisJsonCache<Observation>` |
| `weather.cache.alerts.hits` / `weather.cache.alerts.misses` | Counter | — | `RedisJsonCache<AlertsPayload>` |
| `weather.cache.afd.hits` / `weather.cache.afd.misses` | Counter | — | `RedisJsonCache<AfdProduct>` |
| `weather.cache.sun.hits` / `weather.cache.sun.misses` | Counter | — | `RedisJsonCache<SunTimes>` |
| `weather_provider_geocoding_seconds` | Timer | `outcome=success|failure` | `NominatimGeocodingProvider` |
| `weather_provider_nws_seconds` | Timer | `outcome=success|failure` | `NwsWeatherProvider` and siblings |

Every metric carries the common tag `service=weather-wrapper-service` (set in `MetricsConfig`).

**Hit-ratio derivation:** `weather.cache.{prefix}.hits / (hits + misses)`.

**Example:**

```bash
$ curl -s http://localhost:8080/actuator/prometheus | grep weather_

# HELP weather.cache.weather.hits Cache hits for weather namespace
# TYPE weather.cache.weather.hits counter
weather_cache_weather_hits_total 2.0
# HELP weather.cache.weather.misses Cache misses for weather namespace
# TYPE weather.cache.weather.misses counter
weather_cache_weather_misses_total 1.0
# HELP weather_provider_geocoding_seconds Time spent calling Nominatim
# TYPE weather_provider_geocoding_seconds summary
weather_provider_geocoding_seconds_count{outcome="success",service="weather-wrapper-service"} 1
```
