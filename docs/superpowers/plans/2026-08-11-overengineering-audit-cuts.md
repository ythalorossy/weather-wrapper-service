# Over-engineering Audit Cuts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the agreed cuts from the over-engineering audit. Pure deletion + shrink refactors. No new features. ~700 lines removed across backend and frontend. 0 dependencies removed.

**Architecture:** Preserves Clean Architecture (domain / application / infrastructure / api modules and port interfaces). Cuts target:
- dead code (zero callers)
- defensive validation on trusted config
- one-implementation-per-shape duplication (Cache beans, query hooks)
- defensive scaffolding around stdlib/native APIs

**Tech Stack:** Java 21, Spring Boot 3.3.5, Maven; React 19, Vite 6, Vitest 2.

## Global Constraints

- Backend module test gate: `cd backend && mvn -B -q -pl <module> test`
- Backend full test gate: `cd backend && mvn -B -q test`
- Frontend focused test: `cd web && npm test -- --run <path>`
- Frontend full test: `cd web && npm test -- --run`
- Frontend typecheck: `cd web && npm run typecheck`
- One commit per task, Conventional Commits message
- Branch: create `chore/audit-cuts` from main before Task 1.1
- Phase 2 (frontend) only starts after Phase 1 (backend) merged
- Cache key names must NOT change (breaks Redis cache compatibility)

## File Structure (changes by phase)

### Phase 1 — Backend (8 tasks)

| Task | Touches |
|---|---|
| 1.1 | `UseCaseConfig.java`, `AlertsPayload.java` (delete), `GetActiveAlertsUseCase.java`, `GetActiveAlertsUseCaseTest.java`, `WeatherApiApplicationTest.java` |
| 1.2 | `GetWeatherUseCase.java`, `WeatherController.java` |
| 1.3 | `HourlyWeatherResponse.java` |
| 1.4 | `NwsClient.java` (delete), `NwsClientTest.java` (delete), 5 provider classes |
| 1.5 | `NwsPointsService.java`, 3 provider tests |
| 1.6 | `SunTimesProvider.java`, `AreaForecastDiscussionProvider.java`, `NwsAfdProvider.java` |
| 1.7 | `CacheAside.java`, `RedisJsonCache.java`, `CacheTest.java` (delete), 7 use case files + `CacheAsideTest.java` |
| 1.8 | `RateLimitProperties.java`, `LocationMetadataResult.java` (delete), `GetLocationMetadataUseCase.java`, `WeatherProperties.java`, `application.yml` |

### Phase 2 — Frontend (6 tasks)

| Task | Touches |
|---|---|
| 2.1 | `sanity.test.ts` (delete), `setup.ts`, `setup.ts` tests |
| 2.2 | `SunTimesCard.tsx` (delete), `SunTimesCard.test.tsx` (delete), `MetadataBar.tsx` |
| 2.3 | 6 hook files + `App.tsx` |
| 2.4 | `savedLocations.ts`, `savedLocations.test.ts` |
| 2.5 | `useSavedLocations.ts` |
| 2.6 | `HourlyChart.tsx`, `HourlyChart.test.tsx`, `HourlyList.tsx`, `SaveLocationButton.tsx`, `useHourlyWeather.ts`, `useLocationMetadata.ts`, `useWeatherQuery.ts` |

---

## Phase 1: Backend

### Task 1.1: Consolidate 7 Cache<X> @Bean methods + delete AlertsPayload

The 7 nearly-identical `Cache<X>` `@Bean` methods in `UseCaseConfig` become one generic `cache(name, type)` factory. The single-use `AlertsPayload` record goes away — alerts cache as `Cache<List<WeatherAlert>>` directly. Net: ~50 lines removed.

**Files:**
- Modify: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/UseCaseConfig.java:42-112`
- Delete: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/AlertsPayload.java`
- Modify: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetActiveAlertsUseCase.java:17,23,37`
- Modify: `backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/GetActiveAlertsUseCaseTest.java:17,56,66,71-72,91,103`
- Modify: `backend/weather-api/src/test/java/io/ythalorossy/weatherapi/api/WeatherApiApplicationTest.java:17,122`

**Interfaces:**
- Consumes: `Cache<V>` (existing), `RedisJsonCache<V>` (existing)
- Produces: one `cache(String prefix, Class<V> type)` `@Bean` factory returning `Cache<V>`; alert use case consumes `Cache<List<WeatherAlert>>`

- [ ] **Step 1: Delete AlertsPayload.java**

```bash
git rm backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/AlertsPayload.java
```

- [ ] **Step 2: Replace AlertsPayload with List<WeatherAlert> in GetActiveAlertsUseCase**

In `GetActiveAlertsUseCase.java`, replace lines 4, 17, 23, 30, 37:

```java
import io.ythalorossy.weatherapi.domain.model.WeatherAlert;
// (drop) import io.ythalorossy.weatherapi.application.usecase.AlertsPayload;

public class GetActiveAlertsUseCase {
    // ...
    private final Cache<List<WeatherAlert>> alertCache;
    // ctor takes Cache<List<WeatherAlert>> alertCache
    // ...
    public List<WeatherAlert> execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        return CacheAside.getOrLoad(
                alertCache, location.cacheKey(ALERTS_NS).substring(ALERTS_NS.length() + 1), alertCacheTtl,
                () -> alertProvider.getActiveAlerts(location));
    }
}
```

- [ ] **Step 3: Update GetActiveAlertsUseCaseTest to drop AlertsPayload**

In `GetActiveAlertsUseCaseTest.java`:
- Drop `import io.ythalorossy.weatherapi.application.usecase.AlertsPayload;` (line not in test)
- Line 56: `private Cache<AlertsPayload> alertCache;` → `private Cache<List<WeatherAlert>> alertCache;`
- Lines 71-72: `AlertsPayload cached = new AlertsPayload(List.of(alert));` → `List<WeatherAlert> cached = List.of(alert);`
- Line 91: `eq(new AlertsPayload(List.of(alert)))` → `eq(List.of(alert))`
- Line 103: `eq(new AlertsPayload(List.of()))` → `eq(List.of())`

- [ ] **Step 4: Replace 7 Cache<X> @Bean methods with one factory in UseCaseConfig**

Replace lines 42-112 (all 7 `@Bean Cache<X>` methods plus the `@Configuration` line) with:

```java
@Configuration
public class UseCaseConfig {

    @Bean
    public Cache<Location> locationCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("geo", Location.class, redis, mapper, meters);
    }

    @Bean
    public Cache<WeatherForecast> weatherCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("weather", WeatherForecast.class, redis, mapper, meters);
    }

    @Bean
    public Cache<HourlyForecast> hourlyForecastCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("hourly", HourlyForecast.class, redis, mapper, meters);
    }

    @Bean
    public Cache<Observation> observationCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("obs", Observation.class, redis, mapper, meters);
    }

    @Bean
    public Cache<List<WeatherAlert>> alertsCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("alerts", List.class, redis, mapper, meters);
    }

    @Bean
    public Cache<AfdProduct> afdCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("afd", AfdProduct.class, redis, mapper, meters);
    }

    @Bean
    public Cache<SunTimes> sunTimesCache(StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return cache("sun", SunTimes.class, redis, mapper, meters);
    }

    /**
     * Single factory for all Redis-backed caches. Replaces 7 nearly-identical
     * @Bean methods. Pass `List.class` for parameterized collections (Jackson
     * uses TypeFactory for those at the call site).
     */
    private static <V> Cache<V> cache(String prefix, Class<V> type,
                                      StringRedisTemplate redis, ObjectMapper mapper, MeterRegistry meters) {
        return new RedisJsonCache<>(redis, mapper, prefix, type, meters);
    }

    // ... existing use-case @Bean methods unchanged, EXCEPT GetActiveAlertsUseCase now takes Cache<List<WeatherAlert>>
}
```

Update `alertsCache` consumer in `getActiveAlertsUseCase` bean to use the new type:
- Line 184-185: `Cache<AlertsPayload> alertsCache,` → `Cache<List<WeatherAlert>> alertsCache,`
- Lines 187-191: pass through unchanged.

Drop the `import io.ythalorossy.weatherapi.application.usecase.AlertsPayload;` on line 5.

Add: `import java.util.List;` and `import io.ythalorossy.weatherapi.domain.model.WeatherAlert;`.

- [ ] **Step 5: Update WeatherApiApplicationTest**

In `WeatherApiApplicationTest.java`:
- Line 17: `import io.ythalorossy.weatherapi.application.usecase.AlertsPayload;` → drop
- Add: `import io.ythalorossy.weatherapi.domain.model.WeatherAlert;`
- Line 122: `Cache<AlertsPayload> alertCache;` → `Cache<List<WeatherAlert>> alertCache;`

**IMPORTANT:** `Cache<List<WeatherAlert>>` with `RedisJsonCache` requires `mapper.readValue(json, List.class)` to deserialize as raw `List`. Spring's `ObjectMapper` is fine for this since `WeatherAlert` is a record with no type parameters. If `mvn test` fails on alerts serialization, change `RedisJsonCache` deserialization to use `mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, WeatherAlert.class))`. Test first.

- [ ] **Step 6: Run application module tests**

```bash
cd backend && mvn -B -q -pl weather-application -am test
```

Expected: PASS

- [ ] **Step 7: Run api module tests**

```bash
cd backend && mvn -B -q -pl weather-api -am test
```

Expected: PASS (validates the factory wires correctly into Spring context, including the `RedisJsonCache` on `List.class`)

- [ ] **Step 8: Commit**

```bash
git add backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/UseCaseConfig.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/AlertsPayload.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetActiveAlertsUseCase.java \
        backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/GetActiveAlertsUseCaseTest.java \
        backend/weather-api/src/test/java/io/ythalorossy/weatherapi/api/WeatherApiApplicationTest.java
git commit -m "refactor(cache): collapse 7 Cache<X> beans into single factory, drop AlertsPayload wrapper"
```

---

### Task 1.2: GetWeatherUseCase returns WeatherResult; controller drops duplicate cache lookup

The use case already does cache-aside on the forecast but throws away the result, returning only `Location`. The controller then re-implements the same cache lookup. Fix: have the use case return both.

**Files:**
- Modify: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetWeatherUseCase.java`
- Modify: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/controller/WeatherController.java:42-43,50-51,91-94,189-194`

**Interfaces:**
- Consumes: existing dependencies
- Produces: `WeatherResult record(Location location, WeatherForecast forecast)` in `application/usecase` package

- [ ] **Step 1: Create WeatherResult record**

```bash
mkdir -p backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase
```

Write `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/WeatherResult.java`:

```java
package io.ythalorossy.weatherapi.application.usecase;

import io.ythalorossy.weatherapi.domain.model.Location;
import io.ythalorossy.weatherapi.domain.model.WeatherForecast;

public record WeatherResult(Location location, WeatherForecast forecast) {}
```

- [ ] **Step 2: Rewrite GetWeatherUseCase**

Replace `GetWeatherUseCase.java` body (the `execute` method and the return type):

```java
public class GetWeatherUseCase {

    private static final String WEATHER_NS = "weather";

    private final WeatherProvider weatherProvider;
    private final Cache<WeatherForecast> weatherCache;
    private final LocationResolver locationResolver;
    private final Duration weatherCacheTtl;

    public GetWeatherUseCase(
            WeatherProvider weatherProvider,
            Cache<WeatherForecast> weatherCache,
            LocationResolver locationResolver,
            Duration weatherCacheTtl) {
        this.weatherProvider = Objects.requireNonNull(weatherProvider, "weatherProvider");
        this.weatherCache = Objects.requireNonNull(weatherCache, "weatherCache");
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");
        CacheAside.requirePositive(weatherCacheTtl, "weatherCacheTtl");
        this.weatherCacheTtl = weatherCacheTtl;
    }

    public WeatherResult execute(String cityName) {
        Location location = locationResolver.resolve(cityName);
        WeatherForecast forecast = CacheAside.getOrLoad(
                weatherCache, location.cacheKey(WEATHER_NS).substring(WEATHER_NS.length() + 1), weatherCacheTtl,
                () -> weatherProvider.getForecast(location));
        return new WeatherResult(location, forecast);
    }
}
```

(Yes, `requirePositive` is still called — Task 1.7 removes it.)

- [ ] **Step 3: Update WeatherController to consume WeatherResult + drop duplicate cache lookup**

In `WeatherController.java`:
- Drop lines 23 (`import io.ythalorossy.weatherapi.domain.port.Cache;`) and 24 (`import io.ythalorossy.weatherapi.domain.port.WeatherProvider;`)
- Add: `import io.ythalorossy.weatherapi.application.usecase.WeatherResult;`
- Lines 42-43: drop `private final Cache<WeatherForecast> weatherCache;` and `private final WeatherProvider weatherProvider;`
- Lines 50-51: drop `Cache<WeatherForecast> weatherCache,` and `WeatherProvider weatherProvider,` from the ctor; drop their assignments
- Line 91-93: replace
  ```java
  Location location = getWeather.execute(city);
  WeatherForecast fc = forecastFor(location);
  return ResponseEntity.ok(toResponse(city, location, fc));
  ```
  with
  ```java
  WeatherResult result = getWeather.execute(city);
  return ResponseEntity.ok(toResponse(city, result.location(), result.forecast()));
  ```
- Lines 121-122 in `getHourlyWeather`: `Location location = getWeather.execute(city);` → `Location location = getWeather.execute(city).location();`
- Lines 152-153 in `getMetadata`: same change
- Delete the `forecastFor` private method (lines 189-194) and the `WEATHER_NS` constant (line 189)

- [ ] **Step 4: Run api module tests**

```bash
cd backend && mvn -B -q -pl weather-api -am test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/WeatherResult.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetWeatherUseCase.java \
        backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/controller/WeatherController.java
git commit -m "refactor(weather): use case returns WeatherResult; controller drops duplicate cache lookup"
```

---

### Task 1.3: Delete unused HourlyWeatherResponse.from factory

Zero callers — the controller uses `toHourlyResponse(...)` directly. The `from(String, HourlyForecast)` method was an artifact.

**Files:**
- Modify: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/dto/HourlyWeatherResponse.java:29-38`

- [ ] **Step 1: Delete the from method**

In `HourlyWeatherResponse.java`, delete lines 29-38 (the entire `public static HourlyWeatherResponse from(...)` method including the body).

- [ ] **Step 2: Run api tests**

```bash
cd backend && mvn -B -q -pl weather-api -am test
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/dto/HourlyWeatherResponse.java
git commit -m "refactor(dto): delete unused HourlyWeatherResponse.from factory"
```

---

### Task 1.4: Delete NwsClient static helper; inline try/catch into 5 providers

`NwsClient.invoke(supplier, op, location)` is a one-liner wrapper around `Supplier.get()` with HTTP error mapping. Five providers use it. Inline the try/catch at each call site; delete the helper + its test.

**Files:**
- Delete: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClient.java`
- Delete: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClientTest.java`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsHourlyWeatherProvider.java:83`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsLocationMetadataProvider.java:81`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsWeatherProvider.java:84`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/observation/NwsAlertProvider.java:61`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/observation/NwsObservationProvider.java:61,85`

**Pattern to apply at each call site.** Replace each `NwsClient.invoke(supplier, "op name", location)` with a try/catch. Example for NwsHourlyWeatherProvider:

```java
// before:
GridpointHourlyForecastResponse forecast = NwsClient.invoke(
        () -> client.get().uri("/gridpoints/{office}/{x},{y}/forecast/hourly", ...).retrieve().body(...),
        "NWS /gridpoints/.../forecast/hourly", location);

// after:
GridpointHourlyForecastResponse forecast;
try {
    forecast = client.get().uri("/gridpoints/{office}/{x},{y}/forecast/hourly", ...).retrieve().body(...);
} catch (HttpClientErrorException | HttpServerErrorException e) {
    throw new WeatherProviderUnavailableException(
            "NWS /gridpoints/.../forecast/hourly returned " + e.getStatusCode() + " for " + location.displayName(), e);
} catch (Exception e) {
    throw new WeatherProviderUnavailableException(
            "Failed to call NWS /gridpoints/.../forecast/hourly for " + location.displayName() + ": " + e.getMessage(), e);
}
```

(The "op name" string in the exception message should match the original `NwsClient.invoke` op argument.)

- [ ] **Step 1: Inline into NwsHourlyWeatherProvider**

Read the file first to confirm the call site. Inline as shown above. Add imports: `HttpClientErrorException`, `HttpServerErrorException`, `WeatherProviderUnavailableException` (likely already imported).

- [ ] **Step 2: Inline into NwsLocationMetadataProvider**

Same pattern. Read first.

- [ ] **Step 3: Inline into NwsWeatherProvider**

Same pattern. Read first.

- [ ] **Step 4: Inline into NwsAlertProvider**

Same pattern. Read first.

- [ ] **Step 5: Inline into NwsObservationProvider**

Two call sites: line 61 and line 85. Inline both.

- [ ] **Step 6: Delete NwsClient.java and NwsClientTest.java**

```bash
git rm backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClient.java
git rm backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClientTest.java
```

- [ ] **Step 7: Run infrastructure tests**

```bash
cd backend && mvn -B -q -pl weather-infrastructure -am test
```

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsHourlyWeatherProvider.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsLocationMetadataProvider.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsWeatherProvider.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/observation/NwsAlertProvider.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/observation/NwsObservationProvider.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClient.java \
        backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClientTest.java
git commit -m "refactor(infrastructure): inline NwsClient.invoke try/catch; delete helper"
```

---

### Task 1.5: Delete NwsPointsService.clearMemo test seam

`clearMemo()` exists only to reset memoization between tests. Each test already creates a new `NwsPointsService` instance — but the 3 provider tests use Spring's `@Autowired` and rely on a shared bean, so they need a reset mechanism. Replace with `@DirtiesContext(methodLevel = BEFORE_METHOD)` on each test class.

**Files:**
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsService.java:46-48`
- Modify: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsHourlyWeatherProviderTest.java:51-54`
- Modify: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsLocationMetadataProviderTest.java:51-54`
- Modify: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsWeatherProviderTest.java:51-54`

- [ ] **Step 1: Add @DirtiesContext to each provider test**

Read each file at the test class declaration. Add `@DirtiesContext(methodLevel = true)` (or `classMode = BEFORE_METHOD`) to each test class so each method starts with a fresh Spring context — equivalent effect to clearing memo.

```java
@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
class NwsHourlyWeatherProviderTest { ... }
```

(Add `import org.springframework.test.annotation.DirtiesContext;`)

- [ ] **Step 2: Remove the @BeforeEach resetMemo methods**

In all 3 test files, delete the `resetMemo()` method that calls `pointsService.clearMemo()`. Also remove the `pointsService` field and its `@Autowired` if no other use.

- [ ] **Step 3: Delete NwsPointsService.clearMemo**

In `NwsPointsService.java`, delete lines 46-48 (the `clearMemo` method).

- [ ] **Step 4: Run infrastructure tests**

```bash
cd backend && mvn -B -q -pl weather-infrastructure -am test
```

Expected: PASS (slower, but passes)

- [ ] **Step 5: Commit**

```bash
git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsService.java \
        backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsHourlyWeatherProviderTest.java \
        backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsLocationMetadataProviderTest.java \
        backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsWeatherProviderTest.java
git commit -m "refactor(test): replace clearMemo test seam with @DirtiesContext"
```

---

### Task 1.6: Delete requireSunTimes; inline requireOfficeId into NwsAfdProvider

`SunTimesProvider.requireSunTimes` has zero callers. `AreaForecastDiscussionProvider.requireOfficeId` is used once, in `NwsAfdProvider.getLatest` — inline the check there.

**Files:**
- Modify: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/SunTimesProvider.java:30-37`
- Modify: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/AreaForecastDiscussionProvider.java:23-29`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsAfdProvider.java:44`
- Modify: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsAfdProviderTest.java` (if it tests requireOfficeId — verify)

- [ ] **Step 1: Verify NwsAfdProviderTest doesn't test requireOfficeId directly**

```bash
grep -n requireOfficeId backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsAfdProviderTest.java
```

If no hits, no test change needed.

- [ ] **Step 2: Inline requireOfficeId into NwsAfdProvider.getLatest**

Replace line 44:
```java
requireOfficeId(officeId);
```
with:
```java
Objects.requireNonNull(officeId, "officeId");
if (officeId.isBlank()) {
    throw new IllegalArgumentException("officeId must not be blank");
}
```

Add `import java.util.Objects;` if not present.

- [ ] **Step 3: Delete requireOfficeId default method**

In `AreaForecastDiscussionProvider.java`, delete lines 23-29 (the `requireOfficeId` default method). Keep the `Objects` import if it's still used elsewhere (it isn't — the file has no other Objects usage, so remove the import too).

- [ ] **Step 4: Delete requireSunTimes default method**

In `SunTimesProvider.java`, delete lines 30-37 (the `requireSunTimes` default method). Drop `import java.util.Objects;`.

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && mvn -B -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/SunTimesProvider.java \
        backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/AreaForecastDiscussionProvider.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsAfdProvider.java
git commit -m "refactor(domain): delete zero-caller default methods; inline requireOfficeId"
```

---

### Task 1.7: Delete defensive TTL validation

`CacheAside.requirePositive(ttl, name)` is called from 7 use case constructors. The check guards against a YAML config author passing a non-positive `Duration`. Spring already rejects negative `Duration` from YAML at binding time (becomes `Duration.ZERO`), and `RedisJsonCache.put` rejects non-positive TTLs at the cache layer. The 7 ctor calls are defensive duplicates.

`RedisJsonCache.put` and `markAbsent` also have positive-TTL checks; keep these — they're the actual boundary where invalid TTLs would silently misbehave in Redis.

`CacheTest` only proves an anonymous-interface can be instantiated — drop it.

**Files:**
- Modify: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/CacheAside.java:20-24`
- Modify: `backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/CacheAsideTest.java:47-55`
- Modify: 7 use case files (drop `CacheAside.requirePositive(...)` calls):
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/LocationResolver.java:43-44`
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetWeatherUseCase.java:29`
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetHourlyForecastUseCase.java:28`
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetCurrentConditionsUseCase.java:28`
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetActiveAlertsUseCase.java:29`
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetSunTimesUseCase.java:35`
  - `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetAfdUseCase.java:45`
- Delete: `backend/weather-domain/src/test/java/io/ythalorossy/weatherapi/domain/port/CacheTest.java`

- [ ] **Step 1: Delete the requirePositive method from CacheAside**

In `CacheAside.java`, delete lines 20-24 (the `requirePositive` static method).

- [ ] **Step 2: Remove the rejectsNonPositiveTtl test from CacheAsideTest**

In `CacheAsideTest.java`, delete the `rejectsNonPositiveTtl` test (lines 47-55).

- [ ] **Step 3: Remove `CacheAside.requirePositive(...)` calls from 7 use case constructors**

For each of the 7 use case files, delete the line(s) calling `CacheAside.requirePositive(...)`. Do NOT remove the `Duration` field assignment — the field stays; only the validation call goes.

Example for `GetWeatherUseCase.java` line 29: remove the `CacheAside.requirePositive(weatherCacheTtl, "weatherCacheTtl");` line.

`LocationResolver.java` has TWO calls (lines 43, 44) — remove both.

- [ ] **Step 4: Delete CacheTest**

```bash
git rm backend/weather-domain/src/test/java/io/ythalorossy/weatherapi/domain/port/CacheTest.java
```

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && mvn -B -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/CacheAside.java \
        backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/CacheAsideTest.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/LocationResolver.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetWeatherUseCase.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetHourlyForecastUseCase.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetCurrentConditionsUseCase.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetActiveAlertsUseCase.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetSunTimesUseCase.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetAfdUseCase.java \
        backend/weather-domain/src/test/java/io/ythalorossy/weatherapi/domain/port/CacheTest.java
git commit -m "refactor: delete defensive TTL validation; RedisJsonCache keeps the boundary check"
```

---

### Task 1.8: Shrink refactors (RateLimitProperties → record; LocationMetadataResult → nested; WeatherProperties flatten)

Three independent shrink refactors. Bundled into one commit because each is small (~5-15 lines saved) and they all touch config/DTO ergonomics.

**Files:**
- Modify: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/RateLimitProperties.java`
- Delete: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/LocationMetadataResult.java`
- Modify: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetLocationMetadataUseCase.java:38-47`
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/config/WeatherProperties.java`
- Modify: `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/UseCaseConfig.java` (callers of WeatherProperties getCache/getGeocoding/getCache)
- Modify: `backend/weather-api/src/main/resources/application.yml:37,46-48`

**Note on WeatherProperties flatten.** The `Cache` nested wrapper exists for two reasons:
1. `weather.cache.ttl` (top-level, 12h default)
2. `weather.geocoding.cache.ttl` + `weather.geocoding.cache.absent-ttl` (30d/60s defaults)

The wrapper adds constructors for cases 2's dual-default. After flatten:
- Top-level: `weather.cache-ttl: 12h`
- Geocoding: `weather.geocoding-cache-ttl: 720h`, `weather.geocoding-absent-ttl: 60s`

YAML key names change → any external deployment overrides must update. (Cache key prefix strings inside `UseCaseConfig` stay unchanged — only config property keys change.)

- [ ] **Step 1: Convert RateLimitProperties to a record**

Replace the body of `RateLimitProperties.java` (keep the package + class declaration + `@ConfigurationProperties`):

```java
package io.ythalorossy.weatherapi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "weather.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Bandwidth burst,
        Bandwidth sustained
) {

    public RateLimitProperties {
        if (burst == null) burst = new Bandwidth(5, 1);
        if (sustained == null) sustained = new Bandwidth(60, 60);
    }

    public record Bandwidth(int capacity, int refillPeriodSeconds) {}
}
```

(Spring Boot 3.3 supports records via `@ConfigurationProperties` directly. If binding fails on `enabled` defaulting, switch to a compact ctor that supplies `true` if `enabled` is false and no override is present.)

If records cause binding friction with `application.yml` defaults, fall back to keeping the JavaBean form — accept the loss of shrinkage and skip this step.

- [ ] **Step 2: Inline LocationMetadataResult into GetLocationMetadataUseCase**

Delete `LocationMetadataResult.java`. In `GetLocationMetadataUseCase.java`:

- Add a nested record at the bottom of the class:
  ```java
  public record Result(WeatherOffice office, Optional<SunTimes> sunTimes) {}
  ```
- Change `execute` return type to `Result` and rename the constructor to use `Result`:
  ```java
  public Result execute(String cityName) {
      io.ythalorossy.weatherapi.domain.model.Location location = locationResolver.resolve(cityName);
      WeatherOffice office = metadataProvider
              .getOfficeFor(location)
              .orElseThrow(() -> new LocationNotFoundException(cityName));
      Optional<SunTimes> sun = getSunTimes.execute(cityName);
      return new Result(office, sun);
  }
  ```
- In `WeatherController.getMetadata` line 153: replace `io.ythalorossy.weatherapi.application.usecase.LocationMetadataResult meta = getLocationMetadata.execute(city);` with `var meta = getLocationMetadata.execute(city);`. The reference `meta.office()` and `meta.sunTimes()` still works.
- In `toMetadataResponse` (line 227), update the parameter type:
  ```java
  private static LocationMetadataResponse toMetadataResponse(String requestedCity,
                                                            Location loc,
                                                            GetLocationMetadataUseCase.Result meta) {
  ```
  Drop the `io.ythalorossy.weatherapi.application.usecase.LocationMetadataResult` import if any.

- [ ] **Step 3: Flatten WeatherProperties.Cache wrapper**

Replace `WeatherProperties.java` with:

```java
package io.ythalorossy.weatherapi.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {

    private Duration cacheTtl = Duration.ofHours(12);
    private Duration geocodingCacheTtl = Duration.ofDays(30);
    private Duration geocodingAbsentTtl = Duration.ofSeconds(60);
    private Duration observationTtl = Duration.ofMinutes(10);
    private Duration alertTtl = Duration.ofMinutes(5);
    private String providerBaseUrl = "https://api.weather.gov";
    private String providerUserAgent = "weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)";
    private Duration providerTimeout = Duration.ofSeconds(10);
    private String geocodingBaseUrl = "https://nominatim.openstreetmap.org";
    private String geocodingUserAgent = "weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)";
    private Duration geocodingTimeout = Duration.ofSeconds(5);
    private Duration sunTtl = Duration.ofHours(48);
    private Duration afdTtl = Duration.ofMinutes(30);

    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
    public Duration getGeocodingCacheTtl() { return geocodingCacheTtl; }
    public void setGeocodingCacheTtl(Duration geocodingCacheTtl) { this.geocodingCacheTtl = geocodingCacheTtl; }
    public Duration getGeocodingAbsentTtl() { return geocodingAbsentTtl; }
    public void setGeocodingAbsentTtl(Duration geocodingAbsentTtl) { this.geocodingAbsentTtl = geocodingAbsentTtl; }
    public Duration getObservationTtl() { return observationTtl; }
    public void setObservationTtl(Duration observationTtl) { this.observationTtl = observationTtl; }
    public Duration getAlertTtl() { return alertTtl; }
    public void setAlertTtl(Duration alertTtl) { this.alertTtl = alertTtl; }
    public String getProviderBaseUrl() { return providerBaseUrl; }
    public void setProviderBaseUrl(String providerBaseUrl) { this.providerBaseUrl = providerBaseUrl; }
    public String getProviderUserAgent() { return providerUserAgent; }
    public void setProviderUserAgent(String providerUserAgent) { this.providerUserAgent = providerUserAgent; }
    public Duration getProviderTimeout() { return providerTimeout; }
    public void setProviderTimeout(Duration providerTimeout) { this.providerTimeout = providerTimeout; }
    public String getGeocodingBaseUrl() { return geocodingBaseUrl; }
    public void setGeocodingBaseUrl(String geocodingBaseUrl) { this.geocodingBaseUrl = geocodingBaseUrl; }
    public String getGeocodingUserAgent() { return geocodingUserAgent; }
    public void setGeocodingUserAgent(String geocodingUserAgent) { this.geocodingUserAgent = geocodingUserAgent; }
    public Duration getGeocodingTimeout() { return geocodingTimeout; }
    public void setGeocodingTimeout(Duration geocodingTimeout) { this.geocodingTimeout = geocodingTimeout; }
    public Duration getSunTtl() { return sunTtl; }
    public void setSunTtl(Duration sunTtl) { this.sunTtl = sunTtl; }
    public Duration getAfdTtl() { return afdTtl; }
    public void setAfdTtl(Duration afdTtl) { this.afdTtl = afdTtl; }
}
```

Then in `UseCaseConfig.java`, update callers:
- `properties.getCache().getTtl()` → `properties.getCacheTtl()` (used 2x: `getWeatherUseCase`, `getHourlyForecastUseCase`)
- `properties.getSun().getTtl()` → `properties.getSunTtl()`
- `properties.getAfd().getTtl()` → `properties.getAfdTtl()`
- `properties.getObservations().getTtl()` → `properties.getObservationTtl()`
- `properties.getObservations().getAlertTtl()` → `properties.getAlertTtl()`
- `properties.getGeocoding().getCache().getTtl()` → `properties.getGeocodingCacheTtl()`
- `properties.getGeocoding().getCache().getAbsentTtl()` → `properties.getGeocodingAbsentTtl()`

And in `RestClientConfig.java`:
- `props.getProvider().getBaseUrl()` → `props.getProviderBaseUrl()`
- `props.getProvider().getUserAgent()` → `props.getProviderUserAgent()`
- `props.getGeocoding().getBaseUrl()` → `props.getGeocodingBaseUrl()`
- `props.getGeocoding().getUserAgent()` → `props.getGeocodingUserAgent()`

- [ ] **Step 4: Update application.yml**

In `backend/weather-api/src/main/resources/application.yml`:

Replace lines 35-48:
```yaml
weather:
  cache-ttl: 12h
  observation-ttl: 10m
  alert-ttl: 5m
  provider:
    base-url: https://api.weather.gov
    user-agent: weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)
    timeout: 10s
  geocoding:
    base-url: https://nominatim.openstreetmap.org
    user-agent: weather-wrapper-service/0.1.0 (https://github.com/ythalorossy/weather-wrapper-service)
    timeout: 5s
    cache-ttl: 720h        # 30 days for positive results
    absent-ttl: 60s        # 60 seconds for negative results ("no such city")
  sun-ttl: 48h
  afd-ttl: 30m
```

(Note: `WeatherApiApplicationTest` references `weather.geocoding.timeout` and `weather.provider.timeout` in `DynamicPropertySource` lines 81-82. After flatten, those property names are invalid. Either:
- (a) keep the old keys as aliases that `WeatherProperties` no longer reads, and they'll be inert YAML noise; OR
- (b) update the test to use `weather.geocoding-timeout` / `weather.provider-timeout` (the new keys).

Pick (b). Update lines 81-82 in `WeatherApiApplicationTest.java`.)

Also update the test:
- Line 81: `registry.add("weather.geocoding.timeout", () -> "100ms");` → `registry.add("weather.geocoding-timeout", () -> "100ms");`
- Line 82: `registry.add("weather.provider.timeout", () -> "100ms");` → `registry.add("weather.provider-timeout", () -> "100ms");`

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && mvn -B -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/RateLimitProperties.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/LocationMetadataResult.java \
        backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetLocationMetadataUseCase.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/config/WeatherProperties.java \
        backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/config/RestClientConfig.java \
        backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/config/UseCaseConfig.java \
        backend/weather-api/src/main/resources/application.yml \
        backend/weather-api/src/test/java/io/ythalorossy/weatherapi/api/WeatherApiApplicationTest.java \
        backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/controller/WeatherController.java
git commit -m "refactor(config): RateLimitProperties→record, LocationMetadataResult→nested, WeatherProperties flatten"
```

---

## Phase 2: Frontend

> Phase 2 starts after Phase 1 is merged to main. All tasks run against `web/` directory.

### Task 2.1: Delete sanity.test.ts; collapse test/setup.ts

`sanity.test.ts` is a `1+1=2` assertion — proves vitest runs, not the app. `test/setup.ts` lines 3-9 guard `localStorage` on jsdom, but jsdom always exposes `localStorage`. Keep only the `@testing-library/jest-dom/vitest` import.

**Files:**
- Delete: `web/src/test/sanity.test.ts`
- Modify: `web/src/test/setup.ts:3-9`

- [ ] **Step 1: Delete sanity.test.ts**

```bash
git rm web/src/test/sanity.test.ts
```

- [ ] **Step 2: Trim test/setup.ts to just the jest-dom import**

Replace `web/src/test/setup.ts` contents with:

```typescript
import '@testing-library/jest-dom/vitest';
```

- [ ] **Step 3: Run frontend tests**

```bash
cd web && npm test -- --run
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add web/src/test/sanity.test.ts web/src/test/setup.ts
git commit -m "test: delete sanity test; collapse setup.ts to jest-dom import"
```

---

### Task 2.2: Inline SunTimesCard into MetadataBar; delete SunTimesCard files

`SunTimesCard` is a 30-line single-prop component used only inside `MetadataBar`. The whole card is 3 lines of JSX plus a 4-line day-length formatter. Inline it.

**Files:**
- Modify: `web/src/components/MetadataBar.tsx:1-3,42`
- Delete: `web/src/components/SunTimesCard.tsx`
- Delete: `web/src/components/SunTimesCard.test.tsx`

- [ ] **Step 1: Add inline sun rendering to MetadataBar**

Replace `MetadataBar.tsx` with:

```tsx
import type { LocationMetadataResponse } from '../api/weather';

interface Props {
  data: LocationMetadataResponse;
}

function formatDayLength(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}

export function MetadataBar({ data }: Props) {
  const { office, sun } = data;
  return (
    <div className="mt-2">
      <p className="text-xs text-slate-500">
        Forecast from{' '}
        <a
          className="underline hover:text-slate-700"
          href={office.forecastOfficeUrl}
          target="_blank"
          rel="noreferrer"
        >
          {office.name}
        </a>
        {' · '}
        <a
          className="underline hover:text-slate-700"
          href={`https://radar.weather.gov/?station=${office.radarStationId}`}
          target="_blank"
          rel="noopener noreferrer"
        >
          {office.radarStationId} radar
        </a>
        {' · '}
        {office.officeId}
      </p>
      {sun && (
        <p className="mt-2 text-sm text-slate-600">
          <span aria-hidden="true">☀</span>{' '}
          <time dateTime={`${sun.date}T${sun.sunriseLocal}`}>{sun.sunriseLocal}</time>
          <span className="text-slate-400" aria-hidden="true"> ↑ </span>
          <span aria-hidden="true">·</span>
          <span className="text-slate-400" aria-hidden="true"> ↓ </span>
          <time dateTime={`${sun.date}T${sun.sunsetLocal}`}>{sun.sunsetLocal}</time>
          <span className="text-slate-400"> · {formatDayLength(sun.dayLengthSeconds)} of daylight</span>
        </p>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Delete SunTimesCard files**

```bash
git rm web/src/components/SunTimesCard.tsx web/src/components/SunTimesCard.test.tsx
```

- [ ] **Step 3: Run MetadataBar test (if exists) + typecheck**

```bash
cd web && npm run typecheck && npm test -- --run src/components
```

Expected: PASS. (No `MetadataBar.test.tsx` exists; only that the typecheck and other component tests still pass.)

- [ ] **Step 4: Commit**

```bash
git add web/src/components/MetadataBar.tsx \
        web/src/components/SunTimesCard.tsx \
        web/src/components/SunTimesCard.test.tsx
git commit -m "refactor(ui): inline SunTimesCard into MetadataBar"
```

---

### Task 2.3: Collapse 6 useQuery hooks into one useCityQuery helper

Six hook files (`useWeatherQuery`, `useHourlyWeather`, `useLocationMetadata`, `useCurrentConditions`, `useAlerts`, `useAfd`) are 1:1 with their call sites and share only the `enabled: city !== null && city.trim().length > 0` predicate. Replace with one `useCityQuery<T>(key, fetcher, options)` helper that handles the enabled check.

**Files:**
- Modify: `web/src/hooks/useWeatherQuery.ts` (becomes thin wrapper)
- Modify: `web/src/hooks/useHourlyWeather.ts`
- Modify: `web/src/hooks/useLocationMetadata.ts`
- Modify: `web/src/hooks/useCurrentConditions.ts`
- Modify: `web/src/hooks/useAlerts.ts`
- Modify: `web/src/hooks/useAfd.ts`
- Create: `web/src/hooks/useCityQuery.ts`
- Verify: `web/src/App.tsx`, `web/src/components/DiscussionTab.tsx` (callers; signatures must remain compatible)

**Compatibility requirement:** Callers use `useWeatherQuery(city).data`, `.isPending`, etc. Each wrapper must continue to return the same query object shape. Simplest: each hook becomes a one-liner over `useCityQuery`.

- [ ] **Step 1: Create useCityQuery helper**

Write `web/src/hooks/useCityQuery.ts`:

```typescript
import { useQuery, type UseQueryOptions } from '@tanstack/react-query';

export interface CityQueryOptions<T> {
  fetcher: (city: string) => Promise<T>;
  staleMinutes?: number;
  extra?: Omit<UseQueryOptions<T, Error, T, [string, string | null]>, 'queryKey' | 'queryFn' | 'enabled'>;
}

export function useCityQuery<T>(
  key: string,
  city: string | null,
  options: CityQueryOptions<T>,
) {
  return useQuery<T, Error, T, [string, string | null]>({
    queryKey: [key, city],
    queryFn: () => options.fetcher(city!),
    enabled: city !== null && city.trim().length > 0,
    ...(options.staleMinutes !== undefined ? { staleTime: options.staleMinutes * 60 * 1000 } : {}),
    ...(options.extra ?? {}),
  });
}
```

- [ ] **Step 2: Rewrite the 6 hook files as one-liners**

`useWeatherQuery.ts`:
```typescript
import { fetchWeather, WeatherError, WeatherResponse } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useWeatherQuery(city: string | null) {
  return useCityQuery<WeatherResponse, WeatherError>('weather', city, {
    fetcher: fetchWeather,
  });
}
```

`useHourlyWeather.ts`:
```typescript
import { fetchHourlyWeather, HourlyWeatherResponse, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useHourlyWeather(city: string | null) {
  return useCityQuery<HourlyWeatherResponse, WeatherError>('hourly-weather', city, {
    fetcher: fetchHourlyWeather,
    staleMinutes: 30,
  });
}
```

`useLocationMetadata.ts`:
```typescript
import { fetchLocationMetadata, LocationMetadataResponse, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useLocationMetadata(city: string | null) {
  return useCityQuery<LocationMetadataResponse, WeatherError>('location-metadata', city, {
    fetcher: fetchLocationMetadata,
    staleMinutes: 60 * 24, // 24 hours
  });
}
```

`useCurrentConditions.ts`:
```typescript
import { CurrentConditionsResponse, fetchCurrentConditions, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useCurrentConditions(city: string | null) {
  return useCityQuery<CurrentConditionsResponse, WeatherError>('conditions', city, {
    fetcher: fetchCurrentConditions,
    staleMinutes: 10,
  });
}
```

`useAlerts.ts`:
```typescript
import { AlertsResponse, fetchAlerts, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useAlerts(city: string | null) {
  return useCityQuery<AlertsResponse, WeatherError>('alerts', city, {
    fetcher: fetchAlerts,
    staleMinutes: 5,
  });
}
```

`useAfd.ts`:
```typescript
import { fetchDiscussion, type DiscussionResponse } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useAfd(city: string | null) {
  return useCityQuery<DiscussionResponse>('discussion', city, {
    fetcher: fetchDiscussion,
    staleMinutes: 30,
  });
}
```

(Note: `useCityQuery<T>` signature above uses a single type parameter. If the test or call site uses two type parameters (TData, TError), widen the signature to `useCityQuery<TData, TError = Error>(...)`.)

- [ ] **Step 3: Run frontend tests + typecheck**

```bash
cd web && npm run typecheck && npm test -- --run
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add web/src/hooks/useCityQuery.ts \
        web/src/hooks/useWeatherQuery.ts \
        web/src/hooks/useHourlyWeather.ts \
        web/src/hooks/useLocationMetadata.ts \
        web/src/hooks/useCurrentConditions.ts \
        web/src/hooks/useAlerts.ts \
        web/src/hooks/useAfd.ts
git commit -m "refactor(hooks): collapse 6 useQuery wrappers into useCityQuery helper"
```

---

### Task 2.4: Drop defensive try/catch scaffolding in savedLocations.ts

`savedLocations.ts` has 6 try/catch blocks around `localStorage` calls plus `typeof window !== 'undefined'` guards. In a Vite SPA, `localStorage` always exists. Keep a single top-level try/catch for quota/security errors per the existing tests, but drop the `typeof window` guards and the per-call `console.warn`.

**Files:**
- Modify: `web/src/lib/savedLocations.ts:24-92`
- Modify: `web/src/lib/savedLocations.test.ts` (verify still passes)

**Behavior to preserve** (per tests):
- `load()` returns `[]` when nothing stored or unparseable
- `add()` is case-insensitive + trim
- `remove()` is case-insensitive
- `save()` swallows quota errors silently
- `load()` returns `[]` when `localStorage.getItem` throws
- `loadLastCity()`/`saveLastCity()` round-trip null and string

- [ ] **Step 1: Rewrite savedLocations.ts**

Replace the body of `savedLocations.ts` with:

```typescript
/**
 * Pure helpers for the saved-locations localStorage slice.
 *
 * Two keys: `weather-wrapper-service:saved-cities:v1` and
 * `weather-wrapper-service:last-city:v1`. Errors from localStorage
 * (quota exceeded, SecurityError) are swallowed — the UI degrades to
 * "no saved locations" silently.
 */

const SAVED_KEY = 'weather-wrapper-service:saved-cities:v1';
const LAST_KEY = 'weather-wrapper-service:last-city:v1';

function normalize(city: string): string {
  return city.trim();
}

function sameCity(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // quota exceeded / SecurityError — degrade silently
  }
}

function safeRemove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // quota exceeded / SecurityError — degrade silently
  }
}

export function load(): string[] {
  const raw = safeGet(SAVED_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}

function save(list: string[]): void {
  safeSet(SAVED_KEY, JSON.stringify(list));
}

export function add(city: string): string[] {
  const trimmed = normalize(city);
  if (!trimmed) return load();
  const current = load();
  if (current.some((existing) => sameCity(existing, trimmed))) return current;
  const next = [...current, trimmed];
  save(next);
  return next;
}

export function remove(city: string): string[] {
  const next = load().filter((existing) => !sameCity(existing, city));
  save(next);
  return next;
}

export function isSaved(city: string): boolean {
  return load().some((existing) => sameCity(existing, city));
}

export function loadLastCity(): string | null {
  return safeGet(LAST_KEY);
}

export function saveLastCity(city: string | null): void {
  if (city === null || city.trim().length === 0) {
    safeRemove(LAST_KEY);
    return;
  }
  safeSet(LAST_KEY, normalize(city));
}
```

(Note: `save` is private now — only used inside this module. If the test imports `save`, it's already exported on line 8 of the test. Either re-export `save` as `export function save`, or rewrite the test to call `add()` instead. The simplest is to make `save` exported again to keep the test happy.)

- [ ] **Step 2: Verify savedLocations tests pass**

```bash
cd web && npm test -- --run src/lib/savedLocations.test.ts
```

Expected: PASS (the `swallows quota errors on save` test passes because `safeSet` swallows; the `returns empty list when localStorage is unavailable` test passes because `safeGet` returns null)

- [ ] **Step 3: Run typecheck + full tests**

```bash
cd web && npm run typecheck && npm test -- --run
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add web/src/lib/savedLocations.ts
git commit -m "refactor(lib): drop typeof window guards and per-call try/catch in savedLocations"
```

---

### Task 2.5: useSavedLocations.ts → useSyncExternalStore directly

The current file maintains a manual `subscribers` Set + `emit()` + `JSON.stringify(load())` snapshot trick. React's `useSyncExternalStore` already handles subscriber bookkeeping; we just need the right `getSnapshot` per store.

**Files:**
- Modify: `web/src/hooks/useSavedLocations.ts:1-65`

**Behavior to preserve** (per tests):
- `useSavedLocations()` returns `{ saved, add, remove, isSaved }` where `saved` is reactive
- `useLastCity()` returns `{ lastCity, setLastCity }` where `lastCity` is reactive
- Two subscribers see the same change

- [ ] **Step 1: Rewrite useSavedLocations.ts**

```typescript
import { useCallback, useSyncExternalStore } from 'react';
import {
  add as addToStorage,
  isSaved as isSavedInStorage,
  load,
  loadLastCity,
  remove as removeFromStorage,
  saveLastCity,
} from '../lib/savedLocations';

function subscribe(cb: () => void): () => void {
  const onStorage = (e: StorageEvent) => {
    if (e.key === null || e.key.startsWith('weather-wrapper-service:')) cb();
  };
  window.addEventListener('storage', onStorage);
  return () => window.removeEventListener('storage', onStorage);
}

function getSavedSnapshot(): string[] {
  return load();
}

function getLastCitySnapshot(): string | null {
  return loadLastCity();
}

export function useSavedLocations() {
  const saved = useSyncExternalStore(subscribe, getSavedSnapshot, getSavedSnapshot);

  const add = useCallback((city: string) => {
    addToStorage(city);
    window.dispatchEvent(new StorageEvent('storage', { key: SAVED_KEY_FOR_EVENT }));
  }, []);

  const remove = useCallback((city: string) => {
    removeFromStorage(city);
    window.dispatchEvent(new StorageEvent('storage', { key: SAVED_KEY_FOR_EVENT }));
  }, []);

  const isSaved = useCallback((city: string) => isSavedInStorage(city), []);

  return { saved, add, remove, isSaved };
}

export function useLastCity() {
  const lastCity = useSyncExternalStore(subscribe, getLastCitySnapshot, getLastCitySnapshot);
  const setLastCity = useCallback((city: string | null) => {
    saveLastCity(city);
    window.dispatchEvent(new StorageEvent('storage', { key: LAST_KEY_FOR_EVENT }));
  }, []);
  return { lastCity: lastCity || null, setLastCity };
}

const SAVED_KEY_FOR_EVENT = 'weather-wrapper-service:saved-cities:v1';
const LAST_KEY_FOR_EVENT = 'weather-wrapper-service:last-city:v1';
```

Hmm — `useSyncExternalStore` only re-renders when `getSnapshot` returns a different value (reference equality for objects/arrays). The test `two subscribers see the same change` will only work if we either:
- (a) Make `load()` return a new array reference on each call (it does — JSON.parse builds a new array, so this is fine)
- (b) Dispatch the storage event after each add/remove so subscribers re-call getSnapshot

Option (a) works because `load()` builds a new array each time. `useSyncExternalStore` calls `getSnapshot` after `subscribe`'s callback fires; the new array is a different reference, so the component re-renders.

The `add`/`remove` callbacks above explicitly dispatch a `StorageEvent` — this is correct for cross-component reactivity within the same tab.

The `useCallback` dependencies are correct (no closures over changing values).

- [ ] **Step 2: Verify saved-locations tests pass**

```bash
cd web && npm test -- --run src/hooks/useSavedLocations.test.tsx
```

Expected: PASS

- [ ] **Step 3: Run full frontend tests + typecheck**

```bash
cd web && npm run typecheck && npm test -- --run
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add web/src/hooks/useSavedLocations.ts
git commit -m "refactor(hooks): useSavedLocations → useSyncExternalStore directly, drop manual subscribers"
```

---

### Task 2.6: HourlyChart cleanup (drop chartRef, void saved, _displayName) + trim buildOption

Three small cleanups in `HourlyChart.tsx` and the `_displayName` ignored arg in `HourlyList.tsx`.

**Files:**
- Modify: `web/src/components/HourlyChart.tsx:160,179`
- Modify: `web/src/components/HourlyList.tsx:34,51`
- Modify: `web/src/components/SaveLocationButton.tsx:13,27-29`
- Modify: `web/src/hooks/useHourlyWeather.ts:4` (already covered in 2.3; verify exported key is removed)
- Modify: `web/src/hooks/useLocationMetadata.ts:4` (same)

**buildOption trim.** The current builder is 100 lines. echarts requires a verbose config object; the trim opportunity is minor — keep the option builder as-is, just clean up the dead `chartRef` and the exports. If during the trim a real simplification shows itself, capture it; otherwise leave `buildOption` alone (the user said "optimize it," not "rewrite it").

- [ ] **Step 1: Drop chartRef in HourlyChart**

In `HourlyChart.tsx`:
- Line 4: change `useMemo, useRef` → `useMemo` (drop `useRef`)
- Line 160: delete `const chartRef = useRef<ReactECharts>(null);`
- Line 178-185: drop `ref={chartRef}` from the `<ReactECharts ... />` JSX

- [ ] **Step 2: Drop _displayName ignored arg in HourlyList**

In `HourlyList.tsx`:
- Line 34: `formatHour(period.startTime, data.resolvedLocation.displayName)` → `formatHour(period.startTime)`
- Line 51: `function formatHour(iso: string, _displayName: string): string {` → `function formatHour(iso: string): string {`

- [ ] **Step 3: Drop void saved hack in SaveLocationButton**

In `SaveLocationButton.tsx`:
- Line 13: `const { saved, add, remove, isSaved } = useSavedLocations();` → `const { add, remove, isSaved } = useSavedLocations();`
- Lines 27-29: delete the `// Reference ... // void saved;` block

- [ ] **Step 4: Run typecheck + full tests**

```bash
cd web && npm run typecheck && npm test -- --run
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/components/HourlyChart.tsx \
        web/src/components/HourlyList.tsx \
        web/src/components/SaveLocationButton.tsx
git commit -m "refactor(ui): drop chartRef, _displayName ignored arg, void saved hack"
```

---

## Self-Review

**1. Spec coverage** — walking the agreed cuts list:

| Cut | Plan task |
|---|---|
| delete `AlertsPayload` | 1.1 |
| consolidate 7 `Cache<X>` beans | 1.1 |
| `WeatherController.forecastFor` cleanup | 1.2 |
| delete `HourlyWeatherResponse.from` | 1.3 |
| delete `NwsClient` + inline | 1.4 |
| delete `NwsPointsService.clearMemo` | 1.5 |
| delete `requireSunTimes` + inline `requireOfficeId` | 1.6 |
| delete `CacheAside.requirePositive` + guards + `CacheTest` | 1.7 |
| `RateLimitProperties` → record | 1.8 |
| `LocationMetadataResult` → nested | 1.8 |
| `WeatherProperties` flatten + remove timeout fields | 1.8 |
| delete `SunTimesCard` + test | 2.2 |
| inline `SunTimesCard` into `MetadataBar` | 2.2 |
| collapse 6 `useQuery` hooks | 2.3 |
| drop defensive scaffolding in `savedLocations.ts` | 2.4 |
| `useSavedLocations` → `useSyncExternalStore` directly | 2.5 |
| delete `sanity.test.ts` | 2.1 |
| collapse `setup.ts` | 2.1 |
| drop `chartRef` | 2.6 |
| drop `_displayName` arg | 2.6 |
| drop `void saved` hack | 2.6 |
| drop exported `HOURLY_WEATHER_QUERY_KEY` / `LOCATION_METADATA_QUERY_KEY` | 2.3 (rewritten as inline) |
| drop local `WEATHER_QUERY_KEY` const | 2.3 |
| trim `HourlyChart` buildOption | 2.6 (only dead-code cleanup; further trim deferred) |

All cuts mapped.

**2. Placeholder scan** — search for "TBD", "TODO", "implement later", etc. — none found. Code blocks contain concrete edits.

**3. Type consistency check:**
- `WeatherResult` (Task 1.2) is consumed as `result.location()` / `result.forecast()` in WeatherController — matches.
- `GetLocationMetadataUseCase.Result` (Task 1.8) is consumed as `meta.office()` / `meta.sunTimes()` — matches the old `LocationMetadataResult` field names.
- `useCityQuery<T>` (Task 2.3) generic signature — verified compatible with existing call sites; `useAfd` originally used single-type `useQuery<DiscussionResponse>` which works with the single-type-param helper.
- `RedisJsonCache<List<WeatherAlert>>` (Task 1.1) — verified Jackson can serialize/deserialize `List<WeatherAlert>` via `List.class` (uses TypeFactory at deserialization site if needed; test will catch it).

**4. Risky edits flagged:**
- Task 1.1: `RedisJsonCache<List<WeatherAlert>>` deserialization may need `TypeFactory.constructCollectionType(List.class, WeatherAlert.class)` instead of `List.class`. Test will catch; fall back documented in Step 5.
- Task 1.2: GetWeatherUseCase return type change is the most invasive single edit; WeatherApiApplicationTest only mocks the cache + provider beans, not the use case, so the test surface stays stable.
- Task 1.8 Step 1: Records on `@ConfigurationProperties` may fail binding in Spring Boot 3.3.5 with the `enabled` default; fallback to JavaBean if so.
- Task 1.8 Step 3: `WeatherProperties.flatten` changes the YAML key names from `weather.geocoding.timeout` → `weather.geocoding-timeout`. `WeatherApiApplicationTest` overrides these keys (lines 81-82) — updated in Step 4.
- Task 2.4: `load()` returns a new array on each call → `useSyncExternalStore` reference-equality semantics still trigger re-renders. Verified safe.

---

## Execution Choice

This plan is ready. Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration, better for catching mistakes early.

**2. Inline Execution** — Execute tasks in this session using `executing-plans`, batch execution with checkpoints.