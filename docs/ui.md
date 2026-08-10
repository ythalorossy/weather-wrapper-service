# UI (Web app)

The `web/` directory is a standalone Vite + React + TypeScript app that consumes
the Spring Boot API at `http://localhost:8080` (via Vite's `/api` proxy).

## Tech stack

| Layer       | Choice                          |
|-------------|---------------------------------|
| Build       | Vite 6                          |
| UI          | React 19 + TypeScript 5.7       |
| Styling     | Tailwind CSS v4 (`@tailwindcss/vite`) |
| Data        | TanStack Query (React Query) 5  |
| State       | Local React `useState` only (no router) |

## Folder structure

```
web/
├── index.html                  # Vite entry
├── package.json
├── tsconfig.json               # + tsconfig.app.json, tsconfig.node.json
├── vite.config.ts              # /api proxy → http://localhost:8080
└── src/
    ├── main.tsx                # React entry, mounts <App/>
    ├── App.tsx                 # Top-level state + composition
    ├── index.css               # Tailwind imports
    ├── vite-env.d.ts
    ├── api/
    │   └── weather.ts          # Typed fetch helpers for backend endpoints
    ├── components/
    │   ├── SearchForm.tsx
    │   ├── ForecastTabs.tsx    # Daily / Hourly tab switcher
    │   ├── ForecastCard.tsx    # Single forecast period
    │   ├── HourlyList.tsx      # Hourly forecast rows
    │   ├── MetadataBar.tsx     # WFO name + NWS attribution strip
    │   ├── CurrentConditionsCard.tsx  # Temp, wind, humidity, pressure
    │   ├── AlertsBanner.tsx    # Most-severe alert with dismiss
    │   └── AlertList.tsx       # All alerts with severity color coding
    └── hooks/
        ├── useWeatherQuery.ts
        ├── useHourlyWeather.ts
        ├── useLocationMetadata.ts
        ├── useCurrentConditions.ts
        └── useAlerts.ts
```

## Components

| Component | Role |
|---|---|
| `App` | Holds `city` and `dismissedAlertIds: Set<string>` state; wires all five query hooks and renders `SearchForm` + tabs/cards/banner/list |
| `SearchForm` | Text input + submit; sets the city in `App` |
| `ForecastTabs` | Daily / Hourly tab switcher |
| `ForecastCard` | Renders a single forecast period |
| `HourlyList` | Renders hourly forecast rows |
| `MetadataBar` | WFO name + NWS attribution strip |
| `CurrentConditionsCard` | Temperature, wind, humidity, pressure |
| `AlertsBanner` | Most-severe alert with dismiss action |
| `AlertList` | All alerts with severity color coding |

## TanStack Query hooks

| Hook | Endpoint | staleTime | gcTime |
|---|---|---|---|
| `useWeatherQuery(city)` | `GET /api/v1/weather` | (TanStack default) | (TanStack default) |
| `useHourlyWeather(city)` | `GET /api/v1/weather/hourly` | 30 min | (TanStack default) |
| `useLocationMetadata(city)` | `GET /api/v1/weather/metadata` | 24 h | (TanStack default) |
| `useCurrentConditions(city)` | `GET /api/v1/conditions` | 10 min | (TanStack default) |
| `useAlerts(city)` | `GET /api/v1/alerts` | 5 min | (TanStack default) |

`gcTime` is not explicitly set anywhere; TanStack Query v5 default is 5 minutes.

## App-level state

- `city: string | null` — current city, `null` until first submit.
- `dismissedAlertIds: Set<string>` — per-session, not persisted (page refresh resets).

## Running

See [Running locally](running.md#frontend-vite--react) for install/dev/build commands.

## Build

```bash
cd web
npm install
npm run build    # type-check + production bundle into dist/
```

## Saved locations

Users can star a city on the forecast header to add it to a persistent
list shown as pills above the search form. Clicking a pill switches the
active city. The last city searched is restored on next visit.

| localStorage key | Value | Purpose |
|---|---|---|
| `weather-wrapper-service:saved-cities:v1` | `string[]` (JSON) | The set of saved city strings. |
| `weather-wrapper-service:last-city:v1` | `string` | The last city searched; restored on mount. |

Components: `SavedLocationsPills` (the row), `SaveLocationButton` (the
star on the forecast header). Both share state via the
`useSavedLocations` hook (built on `useSyncExternalStore` + a private
pub-sub + native `storage` events for cross-tab sync). All `localStorage`
access is funnelled through `web/src/lib/savedLocations.ts` so the rest
of the app never reads or writes directly.