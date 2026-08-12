# AGENTS.md

Repo-local guidance for OpenCode sessions. Read this before editing — most of what you'll need is one section away.

## Repo shape

- `backend/` — Spring Boot 3.3.5 API, Java 21, Maven multi-module. Modules: `weather-domain` → `weather-application` → `weather-infrastructure`, plus `weather-api` (Spring entry point).
- `web/` — Standalone Vite 6 + React 19 + TypeScript 5.7 + Tailwind v4 + TanStack Query 5. No router; `localStorage` for saved cities + last city.
- `docs/` — Authoritative architecture / API / config / tradeoffs notes. Link, don't duplicate.
- `.agents/skills/` — Repo-local OpenCode skills (`lean-build`, `surgical-patch`, `safe-refactor`, `migration`, `investigate-first`, `verify-and-stop`). `superpowers` skills (brainstorming, TDD, etc.) also load; use them.

## Strict rules

- **Dependency direction is one-way only.** `domain ← application ← infrastructure`, and `domain, application, infrastructure ← api`. `weather-domain` has zero Spring/Jackson/Redis — pure Java. If you import Spring into `weather-domain`, you broke the architecture.
- **Use cases are plain Java.** Spring wiring lives only in `weather-api` (see `UseCaseConfig`). Tests for use cases use JUnit + Mockito + AssertJ, no `@SpringBootTest`.
- **Errors are RFC 9457 `ProblemDetail`.** Add new error types in `GlobalExceptionHandler` (backend/.../api/exception/), not scattered try/catch.
- **Cache ports are generic.** Single `Cache<V>` port in `weather-domain/port/`, one adapter `RedisJsonCache<V>` in `weather-infrastructure/cache/`. Use cases depend on `Cache<WeatherForecast>`, `Cache<HourlyForecast>`, etc. Cache-aside boilerplate goes through `CacheAside.getOrLoad(...)` and `CacheAside.requirePositive(...)` in `weather-application`.
- **Cache keys are coordinate-based** (`Location.cacheKey(namespace)`, 4-decimal lat/lon). Two spellings of the same city share a slot — that's intentional.

## Commands

### Backend (full stack)
```bash
docker compose up --build        # API :8080 + Redis :6379
curl http://localhost:8080/actuator/health
curl 'http://localhost:8080/api/v1/weather?city=Arlington,%20VA'
```

### Backend (build/test, no Docker)
```bash
cd backend
mvn verify                       # all tests
mvn -pl weather-domain -am test  # one module + deps
mvn -pl weather-infrastructure -Dtest=NwsClientTest test   # one class
mvn -DskipTests package          # JAR only
```

### Frontend
```bash
cd web
npm install
npm run dev                      # http://localhost:5173 (proxies /api → :8080)
npm run build                    # tsc -b --noEmit + vite build
npm run typecheck                # tsc -b --noEmit only
npm test                         # vitest run (jsdom)
npx vitest run src/components/AlertsBanner.test.tsx   # one file
```

## Quirks an agent will miss

- **Vite proxy requires backend running.** `web/vite.config.ts` proxies `/api` → `localhost:8080`. UI dev will silently 504 if backend isn't up. Start backend first.
- **`WeatherApiApplication` component-scans the whole tree.** `@ComponentScan("io.ythalorossy.weatherapi")` picks up adapters + use cases automatically. Don't manually `@Bean`-wire what's already auto-discovered.
- **Virtual threads are on.** `spring.threads.virtual.enabled=true`. Blocking I/O on `RestClient` parks cheaply — don't rewrite to `WebClient`/reactive without a reason.
- **Rate limit fails open.** `RateLimitFilter` allows requests through if Redis is unreachable (logs a warning). Disabling for tests: `weather.rate-limit.enabled=false` in test config.
- **NWS requires a User-Agent.** Default in `application.yml`; override `WEATHER_PROVIDER_USER_AGENT` for any non-toy deployment or NWS returns 403.
- **Nominatim is rate-limited to ~1 req/s** and silently throttles abusive UAs. Negative-cache misses for 60s (`weather.geocoding.absent-ttl`).
- **Apache HttpClient 5 is on the classpath** because Spring's default `SimpleClientHttpRequestFactory` doesn't follow 3xx, and NWS returns 301s to canonicalize lat/lon precision. Don't remove the dep.
- **Tests use Testcontainers (Redis) + WireMock (NWS/Nominatim).** Docker must be available for the full `mvn verify`. For pure unit tests in `weather-domain`/`weather-application`, no Docker needed.
- **`weather/dev.log` and `web/dist/` are gitignored.** Build artifacts and dev-server logs shouldn't be committed.

## Endpoints (summary)

| URL | What |
|---|---|
| `/api/v1/weather?city=...` | Daily forecast |
| `/api/v1/weather/hourly?city=...` | Hourly forecast |
| `/api/v1/weather/metadata?city=...` | WFO + sun times |
| `/api/v1/weather/forecast/discussion?city=...` | Area Forecast Discussion |
| `/api/v1/conditions?city=...` | Current conditions |
| `/api/v1/alerts?city=...` | Active alerts |
| `/v3/api-docs` / `/swagger-ui/index.html` | OpenAPI 3 + Swagger |
| `/actuator/health` / `/actuator/prometheus` | Health + Prometheus scrape |

Full spec, request/response shapes, and status codes: [`docs/api.md`](docs/api.md). Don't paraphrase — link.

## Configuration

All under `weather.*` in `application.yml`. Full env-var override table: [`docs/configuration.md`](docs/configuration.md). Notable: `REDIS_HOST`, `REDIS_PORT`, `SERVER_PORT`, plus all `WEATHER_*` overrides.

## When you need deeper context

Don't read random files. Start here:
- Architecture / request flow: `docs/architecture.md`
- Module boundaries: `docs/module-layout.md`
- Tradeoffs & deliberate omissions: `docs/tradeoffs.md`
- Rate limiting: `docs/rate-limiting.md`
- Concurrency model: `docs/concurrency.md`
- Observability / metrics: `docs/observability.md`
- UI structure: `docs/ui.md`

## Verification before claiming done

- Backend change: `cd backend && mvn verify` (needs Docker for Testcontainers).
- Frontend change: `cd web && npm run typecheck && npm test`.
- API smoke: `docker compose up --build` then `curl http://localhost:8080/actuator/health`.