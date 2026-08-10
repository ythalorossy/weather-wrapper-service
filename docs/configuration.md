# Configuration

All settings live under the `weather.*` tree in `application.yml` and can be
overridden via environment variables (Spring Boot relaxed binding).

| Property                       | Default                            | Env var override         |
|--------------------------------|------------------------------------|--------------------------|
| `weather.cache.ttl`            | `12h`                              | `WEATHER_CACHE_TTL`      |
| `weather.provider.base-url`    | `https://api.weather.gov`          | `WEATHER_PROVIDER_BASE_URL` |
| `weather.provider.user-agent`  | `weather-wrapper-service/0.1.0 (…)` | `WEATHER_PROVIDER_USER_AGENT` |
| `weather.provider.timeout`     | `10s`                              | `WEATHER_PROVIDER_TIMEOUT` |
| `weather.geocoding.base-url`   | `https://nominatim.openstreetmap.org` | `WEATHER_GEOCODING_BASE_URL` |
| `weather.geocoding.user-agent` | `weather-wrapper-service/0.1.0 (…)` | `WEATHER_GEOCODING_USER_AGENT` |
| `weather.geocoding.timeout`    | `5s`                               | `WEATHER_GEOCODING_TIMEOUT` |
| `weather.geocoding.cache.ttl`  | `720h` (30 days)                   | `WEATHER_GEOCODING_CACHE_TTL` |
| `weather.geocoding.cache.absent-ttl` | `60s`                        | `WEATHER_GEOCODING_CACHE_ABSENT_TTL` |
| `weather.sun.cache.ttl`         | `48h`                              | `WEATHER_SUN_CACHE_TTL`        |
| `weather.afd.ttl`              | `30m`                              | `WEATHER_AFD_TTL`             |
| `spring.data.redis.host`       | `localhost`                        | `REDIS_HOST`             |
| `spring.data.redis.port`       | `6379`                             | `REDIS_PORT`             |
| `server.port`                  | `8080`                             | `SERVER_PORT`            |

**Why a User-Agent?** Both NWS and Nominatim require a descriptive `User-Agent`
identifying the caller. NWS returns `403` without one; Nominatim will silently
rate-limit you into oblivion. Change it to your own contact info for any
non-toy deployment.
