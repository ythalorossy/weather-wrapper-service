# Weather Wrapper Service

A Spring Boot API + Vite/React UI that wraps the National Weather Service with Redis-backed cache-aside.

```
Browser  →  http://localhost:5173  (UI)
API      →  http://localhost:8080  (backend)
GET /api/v1/weather?city=Arlington,%20VA   →   forecast JSON
```

Geocoding delegated to Nominatim. Cache keys derived from lat/lon (2 decimals).

## Quick start

```bash
docker compose up --build     # API on :8080, Redis on :6379
cd web && npm install && npm run dev   # UI on :5173
```

Open <http://localhost:5173>, type a city (e.g. `Arlington, VA`), and the forecast appears.

## Architecture at a glance

```
[UI: Vite + React + TanStack Query]
        ↓ HTTP /api/v1/weather?city=…
[API: Spring Boot] → [Redis] → [Nominatim] / [NWS]
```

Full architecture: [docs/architecture.md](docs/architecture.md).

## Documentation

- [Architecture](docs/architecture.md)
- [Use cases](docs/use-cases.md)
- [Tech stack](docs/tech-stack.md)
- [Module layout](docs/module-layout.md)
- [UI](docs/ui.md)
- [Running locally](docs/running.md)
- [Configuration](docs/configuration.md)
- [API reference](docs/api.md)
- [Testing](docs/testing.md)
- [Observability](docs/observability.md)
- [Rate limiting](docs/rate-limiting.md)
- [Concurrency](docs/concurrency.md)
- [Tradeoffs & future work](docs/tradeoffs.md)
- [Milestones](docs/milestones.md)

## License

[MIT](LICENSE) — Copyright (c) 2026 Ythalo Rossy Saldanha Lira.
