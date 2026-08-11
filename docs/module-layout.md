# Module layout

```
backend/
├── pom.xml                          # Parent POM (dependency & plugin management)
├── weather-domain/                  # Pure domain: value objects, port interfaces, exceptions
│   └── src/main/java/io/ythalorossy/weatherapi/domain/
│       ├── model/                   # Location, Temperature, ForecastPeriod, WeatherForecast, SunTimes, …
│       ├── port/                    # GeocodingProvider, WeatherProvider, Cache<V>  (single generic cache port)
│       └── exception/               # LocationNotFoundException, WeatherProviderUnavailableException
│
├── weather-application/             # Use cases (plain Java, no Spring)
│   └── src/main/java/io/ythalorossy/weatherapi/application/
│       └── usecase/                 # GetWeatherUseCase + siblings + CacheAside helper + AlertsPayload
│
├── weather-infrastructure/          # Adapters implementing domain ports
│   └── src/main/java/io/ythalorossy/weatherapi/infrastructure/
│       ├── config/                  # WeatherProperties, RestClientConfig (dedup'd NWS + Nominatim beans)
│       ├── geocoding/               # NominatimGeocodingProvider + DTOs
│       ├── weather/                 # NwsWeatherProvider + NwsPointsService + NwsClient (shared /points lookup + HTTP-error wrapper)
│       ├── observation/             # NwsObservationProvider, NwsAlertProvider
│       ├── sun/                     # ComputedSunTimesProvider (solarpositioning)
│       └── cache/                   # RedisJsonCache<V> (single generic adapter; Jackson serialization + hit/miss metrics)
│
└── weather-api/                     # Spring Boot application: REST controller, config, entry point
    └── src/main/java/io/ythalorossy/weatherapi/api/
        ├── WeatherApiApplication.java
        ├── controller/              # WeatherController, ConditionsController, AlertsController
        ├── dto/                     # WeatherResponse, HourlyWeatherResponse, CurrentConditionsResponse, …
        ├── exception/               # GlobalExceptionHandler (RFC 9457 ProblemDetail)
        └── config/                  # UseCaseConfig (wires plain-Java use cases + Cache<V> beans into Spring)

web/                                 # UI: Vite + React + TanStack Query + Tailwind v4
```

The dependency graph is strictly one-way:
`domain ← application ← infrastructure` and `domain, application, infrastructure ← api`.
Domain has no Spring, no Jackson, no Redis — just Java.

## Cache layer (post-audit)

All cache ports and adapters collapse to one generic pair:

| Layer | Type | What it does |
|---|---|---|
| Domain port | `Cache<V>` | `Optional<V> get(String key)` + `void put(String, V, Duration)` |
| Infrastructure adapter | `RedisJsonCache<V>` | Jackson serialization, hits/misses meters, configurable prefix per bean |

Use cases depend on `Cache<WeatherForecast>`, `Cache<HourlyForecast>`, `Cache<Observation>`, `Cache<AlertsPayload>`, `Cache<AfdProduct>`, `Cache<SunTimes>`, `Cache<Location>` — seven parameterizations of one port, wired with one adapter. Cache keys are built via `Location.cacheKey(namespace)` (4-decimal precision) for coordinate-based caches, or `Location.geocodingCacheKey(cityName)` for the geocoding cache.

Cache-aside boilerplate is shared via `CacheAside.getOrLoad(cache, key, ttl, loader)` and `CacheAside.requirePositive(ttl, name)` (also rejects `Duration.ZERO`).
