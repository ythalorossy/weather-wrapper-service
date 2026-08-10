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

Registered in the cache adapters and upstream providers:

| Metric | Type | Tags | Where |
|---|---|---|---|
| `weather_cache_location_total` | Counter | `result=hit|miss|negative_hit` | `RedisLocationCache` |
| `weather_cache_weather_total` | Counter | `result=hit|miss` | `RedisWeatherCache` |
| `weather_provider_geocoding_seconds` | Timer | `outcome=success|failure` | `NominatimGeocodingProvider` |
| `weather_provider_nws_seconds` | Timer | `outcome=success|failure` | `NwsWeatherProvider` |

Every metric carries the common tag `service=weather-wrapper-service` (set in `MetricsConfig`).

**Hit-ratio derivation:** `weather_cache_*{result="hit"} / (hit + miss)`. A negative cache hit also increments `miss`, so the math is consistent.

**Example:**

```bash
$ curl -s http://localhost:8080/actuator/prometheus | grep weather_

# HELP weather_cache_location_total Geocoding cache lookups by outcome
# TYPE weather_cache_location_total counter
weather_cache_location_total{result="hit"} 2.0
weather_cache_location_total{result="miss"} 1.0
weather_cache_location_total{result="negative_hit"} 0.0
# HELP weather_cache_weather_total Weather cache lookups by outcome
# TYPE weather_cache_weather_total counter
weather_cache_weather_total{result="hit"} 2.0
weather_cache_weather_total{result="miss"} 0.0
# HELP weather_provider_geocoding_seconds Time spent calling Nominatim
# TYPE weather_provider_geocoding_seconds summary
weather_provider_geocoding_seconds_count{outcome="success",service="weather-wrapper-service"} 1
```
