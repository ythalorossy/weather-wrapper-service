# Use case sequences

Trace each use case as a sequence diagram — the dependency wiring and
branching are easier to follow visually than reading the code alone.
As new use cases land in `weather-application/`, add a sub-section here
following the same pattern: happy-path diagram + path matrix + (optional)
focused diagrams for tricky branches.

## End-to-end UI request

Single submit → forecast rendered. Three cache layers along the way:
**TanStack Query** in the browser (keyed on city, 30 min), **Redis** in
the backend (geocoding 30 d + weather 12 h, plus negative caching on
misses), and **NWS** upstream (we don't cache NWS responses — Redis
backs the *wrapper's* responses).

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as SearchForm<br/>(React)
    participant App as App.tsx
    participant TQ as TanStack Query
    participant API as WeatherController<br/>(Spring Boot)
    participant UC as GetWeatherUseCase
    participant LC as Cache<Location><br/>(Redis)
    participant GP as Nominatim
    participant WC as Cache<WeatherForecast><br/>(Redis)
    participant NWS as api.weather.gov

    User->>UI: type "Arlington, VA" + submit
    UI->>App: setCity("Arlington, VA")
    App->>TQ: useQuery(["weather","Arlington, VA"]) (enabled)
    TQ->>API: GET /api/v1/weather?city=... (via Vite /api proxy)
    API->>UC: execute(city)
    UC->>LC: get(geo:normalized-city)
    alt cache miss
        UC->>GP: GET /search?q=...&format=json
        GP-->>UC: lat/lon + displayName
        UC->>LC: put(geo:city → Location, TTL 30d)
    else cache hit
        LC-->>UC: Location
    end
    UC->>WC: get(weather:{lat:.4f},{lon:.4f})
    alt cache miss
        UC->>NWS: GET /points/{lat},{lon}
        NWS-->>UC: gridId/x/y + forecast zones
        UC->>NWS: GET /gridpoints/{g}/{x},{y}/forecast
        NWS-->>UC: periods[]
        UC->>WC: put(weather:... → forecast, TTL 12h)
    else cache hit
        WC-->>UC: forecast
    end
    UC-->>API: Location (forecast written-through to cache)
    API-->>TQ: 200 WeatherResponse JSON
    TQ->>TQ: store in cache (staleTime 30m, gcTime 60m)
    TQ-->>App: { data, isPending: false }
    App->>User: render <ForecastCard/>
```

### Cache matrix

| Layer | Key | TTL | Hit effect |
|---|---|---|---|
| TanStack Query | `["weather", city]` | staleTime 30 min / gcTime 60 min | Instant re-render, no network |
| `Cache<Location>` (Redis) | `geo:{normalized-city}` | 30 d | Skips Nominatim |
| `Cache<Location>` (Redis) | `absent:geo:{normalized-city}` | 60 s | Skips Nominatim, returns 404 |
| `Cache<WeatherForecast>` (Redis) | `weather:{lat:.4f},{lon:.4f}` | 12 h | Skips both NWS calls |

## `GetWeatherUseCase`

Resolves a city name to a `Location`, then resolves that `Location` to a
`WeatherForecast`. Two-layer cache-aside with negative caching on the
geocoding layer. The code lives in
`backend/weather-application/.../usecase/GetWeatherUseCase.java`.

**Participants** (collaborators injected via constructor):

| Symbol | Role |
|---|---|
| `GeocodingProvider` | Port to Nominatim (city → Location) |
| `WeatherProvider` | Port to NWS (Location → forecast) |
| `Cache<Location>` | Redis, both positive (`geo:*`) and negative (`absent:geo:*`) entries |
| `Cache<WeatherForecast>` | Redis, positive (`weather:*`) entries |

### Happy path: cold cache

Both caches miss on the first request for an unseen city. Every
collaborator is exercised — the most informative single diagram.

```mermaid
sequenceDiagram
    autonumber
    actor UI
    participant Ctrl as WeatherController
    participant UC as GetWeatherUseCase
    participant LC as Cache<Location>
    participant GP as GeocodingProvider
    participant WC as Cache<WeatherForecast>
    participant WP as WeatherProvider

    UI->>Ctrl: GET /api/v1/weather?city=X
    Ctrl->>UC: execute(X)

    Note over UC,LC: Layer 1 — city → Location
    UC->>LC: get("geo:x")
    LC-->>UC: empty
    UC->>LC: isAbsent("geo:x")
    LC-->>UC: false
    UC->>GP: findLocation(X)
    GP-->>UC: Location(lat, lon)
    UC->>LC: put("geo:x", Location, 30d)

    Note over UC,WC: Layer 2 — Location → forecast (write-through)
    UC->>WC: get("weather:lat,lon")
    WC-->>UC: empty
    UC->>WP: getForecast(Location)
    Note over WP: two-step: /points/ → /gridpoints/.../forecast
    WP-->>UC: WeatherForecast(periods)
    UC->>WC: put("weather:lat,lon", forecast, 12h)

    UC-->>Ctrl: Location (controller reads cache for forecast to render)
    Ctrl-->>UI: 200 + JSON
```

### Path matrix

Every legal path through `execute(city)`. Use this as the source of
truth for "what happens if X" questions before reading the code.

| Scenario | `Cache<Location>.get` | `Cache<Location>.isAbsent` | Geocoder | `Cache<WeatherForecast>.get` | Weather | Result |
|---|---|---|---|---|---|---|
| Cold cache (above) | empty | false | called → Location | empty | called → forecast | 200 forecast |
| Geo cached, weather cached | hit | — | — | hit | — | 200 forecast |
| Geo cached, weather miss | hit | — | — | empty | called → forecast | 200 forecast |
| Geo miss → fresh, weather cached | empty | false | called → Location | hit | — | 200 forecast |
| City not found, first time | empty | false | called → empty (markAbsent) | — | — | 404 |
| City not found, within absent TTL | empty | true | — | — | — | 404 (from cache) |
| City not found, after absent TTL | empty | false | called again | — | — | 404 + re-mark |
| NWS down (any cache state) | (varies) | (varies) | (varies) | empty | throws | 502 |
| Blank / null `city` param | — | — | — | — | — | 400 (validation) |

### Negative caching — protecting Nominatim

How the absent-cache layer turns a bad-city flood (typos, scanner probes,
scripted abuse) into a one-shot cost. Two requests for the same
unknown city within 60 s; the second one never reaches Nominatim.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Ctrl as Controller
    participant UC as UseCase
    participant LC as Cache<Location>
    participant GP as GeocodingProvider

    Note over Client,GP: Request #1 — Nominatim absorbs the miss

    Client->>Ctrl: GET /weather?city=asdf
    Ctrl->>UC: execute("asdf")
    UC->>LC: get("geo:asdf")
    LC-->>UC: empty
    UC->>LC: isAbsent("geo:asdf")
    LC-->>UC: false
    UC->>GP: findLocation("asdf")
    GP-->>UC: empty
    UC->>LC: markAbsent("geo:asdf", 60s)
    UC-->>Ctrl: throw LocationNotFoundException
    Ctrl-->>Client: 404

    Note over LC: 60 s TTL on absent:geo:asdf

    Note over Client,GP: Request #2 (within 60 s) — Nominatim untouched

    Client->>Ctrl: GET /weather?city=asdf
    Ctrl->>UC: execute("asdf")
    UC->>LC: get("geo:asdf")
    LC-->>UC: empty
    UC->>LC: isAbsent("geo:asdf")
    LC-->>UC: true
    UC-->>Ctrl: throw LocationNotFoundException (no geocoder call)
    Ctrl-->>Client: 404
```
