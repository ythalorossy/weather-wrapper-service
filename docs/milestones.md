# Milestones

| Milestone | Status | Summary |
|---|---|---|
| **M1** — Forecast expansion | ✅ Done | Daily + hourly forecast, WFO metadata, all with Redis cache-aside |
| **M2** — Observations + alerts | ✅ Done | Current conditions (10 min cache), active alerts (5 min cache), severity colors in UI |
| **M3** — Multi-location + chart polish | ✅ Done | localStorage saved locations, hand-rolled hourly temperature SVG chart |
| **M4** — Polish & niche (optional) | ○ Future | Radar station nearby, AFD text, glossary tooltips |

## M1 — Forecast expansion ✅ (2026-08-08)

| Endpoint | Cache TTL | Key |
|---|---|---|
| `GET /api/v1/weather?city=…` (daily) | 12 h | `weather:{lat:.2f},{lon:.2f}` |
| `GET /api/v1/weather/hourly?city=…` | 12 h | `hourly:{lat:.2f},{lon:.2f}` |
| `GET /api/v1/weather/metadata?city=…` | 7 d | `office:{officeId}` |

**Backend:** `GetHourlyForecastUseCase`, `GetLocationMetadataUseCase`, Redis adapters,
`RedisHourlyForecastCache`, `RedisLocationMetadataCache`, `NwsOfficeMetadataProvider`.

**Frontend:** `ForecastTabs` component (Daily / Hourly tabs), `MetadataBar` with WFO name
and NWS attribution. TanStack Query hooks: `useWeatherQuery`, `useHourlyWeather`,
`useLocationMetadata`.

**Note:** sunrise/sunset data types exist in the domain layer but are not yet wired
to an NWS endpoint — deferred to the M3 bundle.

## M2 — Observations + alerts ✅ (2026-08-09)

| Endpoint | Cache TTL | Key |
|---|---|---|
| `GET /api/v1/conditions?city=…` | 10 min | `obs:{stationId}` |
| `GET /api/v1/alerts?city=…` | 5 min | `alerts:{lat:.2f},{lon:.2f}` |

**Backend:** `GetCurrentConditionsUseCase`, `GetActiveAlertsUseCase`,
`NwsObservationProvider`, `NwsAlertProvider`, `RedisObservationCache`, `RedisAlertCache`.
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

## M4 — Polish & niche ○ Future

- Radar station nearby (NWS radar station lookup)
- Area Forecast Discussion (AFD) — raw NWS forecast discussion text
- Glossary tooltips for weather terms