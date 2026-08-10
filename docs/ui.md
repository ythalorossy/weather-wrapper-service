# UI (Web app)

The `web/` directory is a standalone Vite + React + TypeScript app that consumes
the Spring Boot API.

## Tech stack

| Layer       | Choice                          |
|-------------|---------------------------------|
| Build       | Vite 6                          |
| UI          | React 18 + TypeScript           |
| Styling     | Tailwind CSS v4                 |
| Data        | TanStack Query (React Query) 5  |
| Routing     | Single-page, query-string driven |

## Folder structure

```
web/
├── index.html               # Vite entry
├── package.json
├── tsconfig.json
├── vite.config.ts           # /api proxy → http://localhost:8080
└── src/
    ├── main.tsx             # React entry
    ├── App.tsx              # Top-level component, search form, forecast rendering
    ├── index.css            # Tailwind imports
    └── vite-env.d.ts
```

## Components

| Component | Role |
|---|---|
| `App` | Top-level: search form, current city, renders tabs |
| `ForecastTabs` | Daily / Hourly tab switcher |
| `MetadataBar` | WFO name + NWS attribution strip |
| `ForecastCard` | Renders a single forecast period |
| `CurrentConditionsCard` | Temperature, wind, humidity, pressure |
| `AlertsBanner` | Most-severe alert with dismiss |
| `AlertList` | All alerts with severity color coding |

## TanStack Query hooks

| Hook | Endpoint | staleTime | gcTime |
|---|---|---|---|
| `useWeatherQuery(city)` | `GET /api/v1/weather` | 30 min | 60 min |
| `useHourlyWeather(city)` | `GET /api/v1/weather/hourly` | 30 min | 60 min |
| `useLocationMetadata(city)` | `GET /api/v1/weather/metadata` | 30 min | 60 min |
| `useCurrentConditions(city)` | `GET /api/v1/conditions` | 30 min | 60 min |
| `useAlerts(city)` | `GET /api/v1/alerts` | 30 min | 60 min |

## Running

See [Running locally](running.md#frontend-vite--react) for install/dev/build commands.

## Build

```bash
cd web
npm install
npm run build    # type-check + production bundle into dist/
```