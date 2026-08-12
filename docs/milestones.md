# Milestones

| Milestone | Status | Summary |
|---|---|---|
| **M1** — Forecast expansion | ✅ Done | Daily + hourly forecast, WFO metadata, all with Redis cache-aside |
| **M2** — Observations + alerts | ✅ Done | Current conditions (10 min cache), active alerts (5 min cache), severity colors in UI |
| **M3** — Multi-location + chart polish | ✅ Done | localStorage saved locations, hand-rolled hourly temperature SVG chart |
| **M4** — Polish & niche (optional) | ✅ Done | Radar station link, AFD endpoint + Discussion tab, Glossary tooltips |
| **M5** — Over-engineering audit | ✅ Done | Generic `Cache<V>` port collapses 6 typed cache ports + 7 Redis adapters; NWS dedup; DTO/model shrinks; web cleanup. ~1980 LOC removed. |
| **M6** — Radar map embed | ✅ Done | `RadarCard` with lazy iframe (RainViewer); button keeps the dashboard free of third-party scripts on first paint. |

## M1 — Forecast expansion ✅ (2026-08-08)

| Endpoint | Cache TTL | Key |
|---|---|---|
| `GET /api/v1/weather?city=…` (daily) | 12 h | `weather:{lat:.4f},{lon:.4f}` |
| `GET /api/v1/weather/hourly?city=…` | 12 h | `hourly:{lat:.4f},{lon:.4f}` |
| `GET /api/v1/weather/metadata?city=…` | 7 d | `afd`/`sun` cached by location + date/office |

**Backend:** `GetHourlyForecastUseCase`, `GetLocationMetadataUseCase`, `RedisJsonCache<V>` adapters,
`NwsOfficeMetadataProvider`.

**Frontend:** `ForecastTabs` component (Daily / Hourly tabs), `MetadataBar` with WFO name
and NWS attribution. TanStack Query hooks: `useWeatherQuery`, `useHourlyWeather`,
`useLocationMetadata`.

**Note:** sunrise/sunset data types exist in the domain layer and are wired to the
NWS sunrise/sunset endpoint; surfaced in `LocationMetadataResponse` (`sun` field); cached 48 h.

## M2 — Observations + alerts ✅ (2026-08-09)

| Endpoint | Cache TTL | Key |
|---|---|---|
| `GET /api/v1/conditions?city=…` | 10 min | `obs:{lat:.4f},{lon:.4f}` |
| `GET /api/v1/alerts?city=…` | 5 min | `alerts:{lat:.4f},{lon:.4f}` |

**Backend:** `GetCurrentConditionsUseCase`, `GetActiveAlertsUseCase`,
`NwsObservationProvider`, `NwsAlertProvider`, `AlertsPayload` (cache wrapper for empty-list caching).
Domain model: `Observation`, `WeatherAlert`, CAP-IP severity/certainty/urgency/category
enums.

**Frontend:** `CurrentConditionsCard` (temp, wind, humidity, pressure), `AlertsBanner`
(most severe alert with dismiss), `AlertList` (all alerts with severity color coding).
TanStack Query hooks: `useCurrentConditions`, `useAlerts`.

## M3 — Multi-location + chart polish ✅

- **Saved locations:** `localStorage`-backed location list — client-side only, no server
  state changes. "Save this location" button on the forecast header.
- **Hourly temperature chart:** hand-rolled SVG line chart (no extra dependency) —
  temperature vs. time for the next 24–48 hours. Embedded in the Hourly tab.
- **Sunrise/sunset:** `SunTimes` domain types wired to NWS sunrise/sunset endpoint;
  surfaced in `LocationMetadataResponse` (`sun` field); cached 48 h.

## M4 — Polish & niche ✅

- Radar station link
- AFD endpoint + Discussion tab
- Glossary tooltips

## M5 — Over-engineering audit ✅ (2026-08-11)

Three coordinated refactors that cut ~1980 LOC and fix a pre-existing test failure, while keeping all metric names, cache keys (modulo precision bump), and HTTP contracts stable.

**Plan A — Generic cache layer.** Replaced 6 typed cache ports (`WeatherCache`, `HourlyForecastCache`, `AlertCache`, `ObservationCache`, `SunTimesCache`, `AfdCache`) + 7 `Redis*Cache` adapters with one `Cache<V>` port + one `RedisJsonCache<V>` adapter. 6 use cases migrated to depend on `Cache<X>`. Cache-aside boilerplate centralized in `CacheAside.getOrLoad(...)` / `CacheAside.requirePositive(...)`.

**Plan B — Backend model / NWS / DTO cleanup.**
- `NwsPointsService` extracted — shared `/points/{lat},{lon}` lookup (3 forecast providers + memoization).
- `NwsClient.invoke(...)` extracted — shared HTTP-error → `WeatherProviderUnavailableException` wrapper (4 providers).
- `Location.cacheKey(namespace)` replaces 3 hand-rolled cache-key methods; precision bumped from 2 → 4 decimals (matches the existing `Location` validation precision, no cache compatibility break since these were just deployed).
- `WeatherQueryResult` deleted; controllers read `.location()` directly from `GetWeatherUseCase`.
- `HourlyWeatherResponse.LocationView`/`TemperatureView` deduplicated against `WeatherResponse`'s copies.
- `ComputedSunTimesProvider`: dropped reflection, replaced with `instanceof SunriseResult.RegularDay r && r.sunrise().isBefore(r.sunset())` boundary check — fixes the pre-existing `returnsEmptyForReykjavikMidSummer` test failure (`solarpositioning` 2.0.x returns degenerate RegularDay at borderline polar latitudes).
- `RestClientConfig` dedup: two `@Bean` methods collapsed via private builder; duplicate Javadoc removed.
- `NominatimGeocodingProvider` URL builder collapsed to one line.
- `WeatherForecast` / `HourlyForecast` investigated, kept split (different fields/period types), documented with `# ponytail:` comments.

**Plan C — Web cleanup + delete sweep.**
- Deleted tracked artifacts: `web/dev.log`, `web/.gitkeep`, `task-2.11-report.md`. Added `.gitignore` patterns for `web/dist/`, `web/dev.log`, `.github/modernize/`.
- Web hook shrinks: dropped unused `ALERTS_QUERY_KEY`/`CONDITIONS_QUERY_KEY` constants; replaced duplicate `isSaved` logic with a direct call to `savedLocations.isSaved(...)`.
- `HourlyChart`: dropped local duplicate `onClickHour` function (kept the exported one).
- `App.tsx`: replaced manual `useState` + `localStorage` listener with the shared `useLastCity` hook (added `setLastCity` to make it usable from `SearchForm`).
- `savedLocations.ts`: inlined `safeGet`/`safeSet`/`safeRemove` wrappers at call sites.
- Merged `DiscussionCard` into `DiscussionTab` (one component owns both the query and the rendering).
- Tests: 38 vitest tests still pass; `npm run build` clean.

**Tests after M5:** 128 backend + 38 web = 166 passing.

## M6 — Radar map embed ✅ (2026-08-12)

Frontend-only slice. New component `web/src/components/RadarCard.tsx`:
- "Show radar map" button by default; iframe (RainViewer, centered on
  `daily.data.resolvedLocation`) mounts on click.
- No new backend work; lat/lon already on every `WeatherResponse`.
- M4 "KLWX radar" link in `MetadataBar` retained for the new-tab
  affordance; both surfaces coexist.

Tests: 37 → 40 web (3 new in `RadarCard.test.tsx`).
