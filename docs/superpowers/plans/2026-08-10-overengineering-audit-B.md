# Plan B — Backend Model / NWS / DTO Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate NWS provider duplication, simplify DTOs and domain models, and remove the pre-existing `ComputedSunTimesProviderTest.returnsEmptyForReykjavikMidSummer` test failure. Eliminates ~300 LOC and one pre-existing test failure, all in backend Java.

**Architecture:**
- New `NwsPointsService` shared by 3 forecast providers (eliminates 3× duplicated `/points` lookup + `PointsResponse` parsing).
- New static `NwsClient.invoke(...)` helper shared by 4 NWS providers (eliminates 4× copy of HTTP-error → `WeatherProviderUnavailableException` translation).
- `Location.cacheKey(String namespace)` replaces 3 hand-rolled cache-key methods.
- `HourlyWeatherResponse.LocationView`/`TemperatureView` deleted; reuse `WeatherResponse`'s.
- `WeatherQueryResult` record deleted; controllers read `.location()` directly.
- `NominatimGeocodingProvider` URL builder collapsed to one line.
- `ComputedSunTimesProvider` reflection replaced with sealed `switch` on `SunriseResult` (which also fixes the Reykjavik test failure).
- `RestClientConfig` dedup: two `@Bean` methods collapsed via private builder, duplicate Javadoc block deleted.
- `WeatherForecast` / `HourlyForecast` merge: investigated but kept split with a `# ponytail:` comment per user direction.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JUnit 5, AssertJ, WireMock.

## Global Constraints

- See [`2026-08-10-overengineering-audit.md`](./2026-08-10-overengineering-audit.md) global constraints.
- For B5: `solarpositioning 2.0.3` exposes `SunriseResult` as a sealed interface with three record subtypes — `RegularDay(sunrise, transit, sunset)`, `AllDay(transit)`, `AllNight(transit)`. Verified by decompiling `~/.m2/repository/net/e175/klaus/solarpositioning/2.0.3/solarpositioning-2.0.3.jar`.
- For B5: pattern-matching `switch` on the sealed type gives compile-time exhaustiveness.

## Files

**Create:**
- `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsService.java`
- `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClient.java`
- Tests: `NwsPointsServiceTest.java`, `NwsClientTest.java`

**Modify:**
- 5 NWS providers: `NwsWeatherProvider`, `NwsHourlyWeatherProvider`, `NwsLocationMetadataProvider`, `NwsObservationProvider`, `NwsAlertProvider`
- `RestClientConfig.java`
- `HourlyWeatherResponse.java`
- `Location.java`
- `WeatherForecast.java`, `HourlyForecast.java` (comment-only — see B7)
- `WeatherQueryResult.java` (deleted; see B4)
- `GetLocationMetadataUseCase.java`
- `GetWeatherUseCase.java`
- `ComputedSunTimesProvider.java` (and its test for the Reykjavik case)
- `NominatimGeocodingProvider.java`
- `WeatherController.java`, `AlertsController.java` (drop `WeatherQueryResult` import)

---

### Task B1 — Extract `NwsPointsService`

**Files:**
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsService.java`
- Create: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsServiceTest.java`

- [ ] **Step 1: Write failing test**

  `NwsPointsServiceTest.java`:
  ```java
  package io.ythalorossy.weatherapi.infrastructure.weather;

  import com.github.tomakehurst.wiremock.WireMockServer;
  import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
  import org.junit.jupiter.api.AfterAll;
  import org.junit.jupiter.api.BeforeAll;
  import org.junit.jupiter.api.Test;
  import org.springframework.web.client.RestClient;
  import static com.github.tomakehurst.wiremock.client.WireMock.*;
  import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
  import static org.assertj.core.api.Assertions.assertThat;

  class NwsPointsServiceTest {

      static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

      @BeforeAll
      static void setup() {
          wireMock.start();
          RestClient client = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
          NwsPointsService.points = (c, lat, lon) -> c;
          // (the above static setter is a test hook — see implementation step 3 below)
      }

      @AfterAll
      static void teardown() { wireMock.stop(); }

      @Test
      void memoizesPointsLookupPerCoordinate() {
          wireMock.stubFor(get(urlMatching("/points/.*"))
                  .willReturn(aResponse().withStatus(200)
                          .withHeader("Content-Type", "application/json")
                          .withBody("""
                              { "properties": { "gridId": "LWX", "gridX": 1, "gridY": 1 } }""")));

          NwsPointsService svc = new NwsPointsService(RestClient.builder()
                  .baseUrl(wireMock.baseUrl()).build());

          PointsResponse a = svc.lookup(38.88, -77.09);
          PointsResponse b = svc.lookup(38.88, -77.09);

          assertThat(a).isSameAs(b); // cached
          wireMock.verify(1, getRequestedFor(urlMatching("/points/.*")));
      }
  }
  ```

  NOTE: revise the implementation in step 3 to use a constructor that takes the `RestClient` (no static setter hack). The above test sketch shows intent; the actual implementation below does not need a static hook.

- [ ] **Step 2: Run test, expect failure**

  Run: `mvn -pl weather-infrastructure test -Dtest=NwsPointsServiceTest`
  Expected: compile error `cannot find symbol: class NwsPointsService`.

- [ ] **Step 3: Implement**

  `NwsPointsService.java`:
  ```java
  package io.ythalorossy.weatherapi.infrastructure.weather;

  import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;
  import org.springframework.stereotype.Component;
  import org.springframework.web.client.RestClient;
  import java.util.Map;
  import java.util.concurrent.ConcurrentHashMap;

  @Component
  public class NwsPointsService {

      private final RestClient client;
      // # ponytail: per-process memoization, single-instance only.
      // For multi-instance deploy, share via Cache<PointsResponse> with prefix "points".
      private final Map<String, PointsResponse> memo = new ConcurrentHashMap<>();

      public NwsPointsService(RestClient nwsRestClient) {
          this.client = nwsRestClient;
      }

      public PointsResponse lookup(double lat, double lon) {
          String key = "%.4f,%.4f".formatted(lat, lon);
          return memo.computeIfAbsent(key, k ->
                  client.get().uri("/points/{lat},{lon}", lat, lon)
                          .retrieve().body(PointsResponse.class));
      }
  }
  ```

  Note on the test in step 1: rewrite it to instantiate `NwsPointsService` directly (no static setter). The actual test:

  ```java
  class NwsPointsServiceTest {
      static WireMockServer wireMock = new WireMockServer(options().dynamicPort());

      @BeforeAll
      static void setup() { wireMock.start(); }

      @AfterAll
      static void teardown() { wireMock.stop(); }

      @Test
      void memoizesPointsLookupPerCoordinate() {
          wireMock.stubFor(get(urlMatching("/points/.*"))
                  .willReturn(aResponse().withStatus(200)
                          .withHeader("Content-Type", "application/json")
                          .withBody("""
                              { "properties": { "gridId": "LWX", "gridX": 1, "gridY": 1 } }""")));

          NwsPointsService svc = new NwsPointsService(
                  RestClient.builder().baseUrl(wireMock.baseUrl()).build());

          PointsResponse a = svc.lookup(38.88, -77.09);
          PointsResponse b = svc.lookup(38.88, -77.09);

          assertThat(a).isSameAs(b);
          wireMock.verify(1, getRequestedFor(urlMatching("/points/.*")));
      }
  }
  ```

- [ ] **Step 4: Run test, expect pass**

  Run: `mvn -pl weather-infrastructure test -Dtest=NwsPointsServiceTest`
  Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsService.java \
          backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsPointsServiceTest.java
  git commit -m "refactor(backend): add NwsPointsService with per-process memoization"
  ```

---

### Task B2 — Migrate 3 NWS providers to `NwsPointsService`

**Files:**
- Modify: `NwsWeatherProvider.java`, `NwsHourlyWeatherProvider.java`, `NwsLocationMetadataProvider.java`

- [ ] **Step 1: Update `NwsWeatherProvider`**

  - Add constructor parameter `NwsPointsService pointsService`.
  - Replace the `/points/{lat},{lon}` block (call to `client.get().uri("/points/{lat},{lon}", ...).retrieve().body(PointsResponse.class)`) with `PointsResponse points = pointsService.lookup(location.latitude(), location.longitude());`.
  - Remove the now-unused `import io.ythalorossy.weatherapi.infrastructure.weather.dto.PointsResponse;` if no other use.

- [ ] **Step 2: Repeat for `NwsHourlyWeatherProvider`**

- [ ] **Step 3: Repeat for `NwsLocationMetadataProvider`**

- [ ] **Step 4: Run tests**

  Run: `mvn -pl weather-infrastructure test`
  Expected: all existing tests pass.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-infrastructure
  git commit -m "refactor(backend): use NwsPointsService from forecast/hourly/metadata providers"
  ```

---

### Task B3 — Extract `NwsClient.invoke(...)` HTTP-error translator

**Files:**
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClient.java`
- Create: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClientTest.java`
- Modify: 4 NWS providers to use `NwsClient.invoke`

- [ ] **Step 1: Write failing test**

  `NwsClientTest.java`:
  ```java
  package io.ythalorossy.weatherapi.infrastructure.weather;

  import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
  import io.ythalorossy.weatherapi.domain.model.Location;
  import org.junit.jupiter.api.Test;
  import org.springframework.web.client.HttpClientErrorException;
  import org.springframework.web.client.HttpServerErrorException;
  import org.springframework.web.client.ResourceAccessException;
  import java.util.function.Supplier;
  import static org.assertj.core.api.Assertions.*;

  class NwsClientTest {

      private static final Location LOC = new Location(38.88, -77.09, "Arlington, VA");

      @Test
      void returnsValueOnSuccess() {
          Supplier<String> s = () -> "ok";
          assertThat(NwsClient.invoke(s, "NWS /foo", LOC)).isEqualTo("ok");
      }

      @Test
      void wraps4xxAsUnavailable() {
          Supplier<String> s = () -> { throw HttpClientErrorException.create(
                  org.springframework.http.HttpStatus.NOT_FOUND, "nf",
                  org.springframework.http.HttpHeaders.EMPTY, null, null); };
          assertThatThrownBy(() -> NwsClient.invoke(s, "NWS /foo", LOC))
              .isInstanceOf(WeatherProviderUnavailableException.class)
              .hasMessageContaining("NWS /foo")
              .hasMessageContaining("404")
              .hasCauseInstanceOf(HttpClientErrorException.class);
      }

      @Test
      void wraps5xxAsUnavailable() {
          Supplier<String> s = () -> { throw HttpServerErrorException.create(
                  org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "ise",
                  org.springframework.http.HttpHeaders.EMPTY, null, null); };
          assertThatThrownBy(() -> NwsClient.invoke(s, "NWS /foo", LOC))
              .isInstanceOf(WeatherProviderUnavailableException.class)
              .hasMessageContaining("500");
      }

      @Test
      void wrapsOtherExceptionsAsUnavailable() {
          Supplier<String> s = () -> { throw new ResourceAccessException("connect refused"); };
          assertThatThrownBy(() -> NwsClient.invoke(s, "NWS /foo", LOC))
              .isInstanceOf(WeatherProviderUnavailableException.class)
              .hasMessageContaining("connect refused");
      }
  }
  ```

- [ ] **Step 2: Run test, expect failure**

  Run: `mvn -pl weather-infrastructure test -Dtest=NwsClientTest`
  Expected: compile error `cannot find symbol: class NwsClient`.

- [ ] **Step 3: Implement**

  `NwsClient.java`:
  ```java
  package io.ythalorossy.weatherapi.infrastructure.weather;

  import io.ythalorossy.weatherapi.domain.exception.WeatherProviderUnavailableException;
  import io.ythalorossy.weatherapi.domain.model.Location;
  import org.springframework.web.client.HttpClientErrorException;
  import org.springframework.web.client.HttpServerErrorException;
  import java.util.function.Supplier;

  public final class NwsClient {

      private NwsClient() {}

      public static <T> T invoke(Supplier<T> call, String op, Location location) {
          try {
              return call.get();
          } catch (HttpClientErrorException | HttpServerErrorException e) {
              throw new WeatherProviderUnavailableException(
                      op + " returned " + e.getStatusCode() + " for " + location.displayName(), e);
          } catch (Exception e) {
              throw new WeatherProviderUnavailableException(
                      "Failed to call " + op + " for " + location.displayName(), e);
          }
      }
  }
  ```

- [ ] **Step 4: Run test, expect pass**

  Run: `mvn -pl weather-infrastructure test -Dtest=NwsClientTest`
  Expected: `Tests run: 4, Failures: 0`.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClient.java \
          backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/weather/NwsClientTest.java
  git commit -m "refactor(backend): add NwsClient.invoke HTTP-error translator"
  ```

---

### Task B4 — Migrate 4 NWS providers to `NwsClient.invoke`

**Files:**
- Modify: `NwsWeatherProvider.java`, `NwsHourlyWeatherProvider.java`, `NwsObservationProvider.java`, `NwsAlertProvider.java`

- [ ] **Step 1: Update `NwsWeatherProvider`**

  - Replace each `invoke(supplier, "NWS /foo", location)` private helper call site with `NwsClient.invoke(...)`.
  - Delete the private `invoke` method.

- [ ] **Step 2: Repeat for the other 3 providers**

- [ ] **Step 3: Run tests**

  Run: `mvn -pl weather-infrastructure test`
  Expected: all existing tests pass.

- [ ] **Step 4: Commit**

  ```bash
  git add backend/weather-infrastructure
  git commit -m "refactor(backend): use NwsClient.invoke from 4 NWS providers"
  ```

---

### Task B5 — `Location` cleanup

**Files:**
- Modify: `backend/weather-domain/.../model/Location.java`

- [ ] **Step 1: Replace the 3 cache-key methods with one**

  Delete `weatherCacheKey()`, `hourlyCacheKey()`, `observationCacheKey()` (and any similar hand-rolled namespace-prefixed key methods). Add one method:
  ```java
  public String cacheKey(String namespace) {
      return namespace + ":%.4f,%.4f".formatted(latitude, longitude);
  }
  ```
  (Format spec preserved: 4 decimal places matches existing behavior.)

- [ ] **Step 2: Update all call sites**

  Run `grep -rn "weatherCacheKey\|hourlyCacheKey\|observationCacheKey" backend/` to find call sites. Replace each `loc.weatherCacheKey()` with `loc.cacheKey("weather")`, etc.

- [ ] **Step 3: Inline the normalization**

  Delete the `WHITESPACE` field and the `normalize(String cityName)` method (or whatever the helper is called). Replace each call site with `cityName.trim().toLowerCase().replaceAll("\\s+", " ")`.

- [ ] **Step 4: Run tests**

  Run: `mvn -pl weather-domain test`
  Expected: all pass.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-domain
  git commit -m "refactor(backend): simplify Location cache-key and normalization"
  ```

---

### Task B6 — DTO and model shrinks

**Files:**
- Modify: `HourlyWeatherResponse.java`, `WeatherController.java`, `AlertsController.java`
- Delete: `WeatherQueryResult.java`
- Modify: `GetLocationMetadataUseCase.java`, `GetWeatherUseCase.java`
- Modify: `NominatimGeocodingProvider.java`

- [ ] **Step 1: Reuse `WeatherResponse.LocationView` / `TemperatureView` in `HourlyWeatherResponse`**

  Delete the inner `LocationView` and `TemperatureView` records in `HourlyWeatherResponse`. Replace usages with `WeatherResponse.LocationView` and `WeatherResponse.TemperatureView`.

- [ ] **Step 2: Delete `WeatherQueryResult`**

  Change `GetWeatherUseCase.execute(...)` to return `Location` directly. Update `WeatherController` and `AlertsController` to read `.location()` from the now-`Location` return value.

- [ ] **Step 3: Drop redundant `resolvedLocation(...)` wrapper**

  In `GetLocationMetadataUseCase`, replace the call to `resolvedLocation(cityName)` (line ~49) with a direct call to the shared resolver; delete the wrapper method.

- [ ] **Step 4: Collapse `NominatimGeocodingProvider` URL builder**

  Replace the `uriBuilder -> ... .build()` chain with:
  ```java
  .uri("/search?q={q}&format=json&limit=1&addressdetails=0", cityName)
  ```

- [ ] **Step 5: Run tests**

  Run: `mvn -pl weather-api test`
  Expected: 25 tests pass.

- [ ] **Step 6: Commit**

  ```bash
  git add backend/
  git commit -m "refactor(backend): simplify DTOs, drop WeatherQueryResult, shrink URL builders"
  ```

---

### Task B7 — `ComputedSunTimesProvider` reflection removal (also fixes Reykjavik test)

**Files:**
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/sun/ComputedSunTimesProvider.java`
- Modify: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/sun/ComputedSunTimesProviderTest.java`

**Background research (done at planning time):**
- `solarpositioning 2.0.3` exposes `SunriseResult` as a sealed interface with three record subtypes: `RegularDay(sunrise, transit, sunset)`, `AllDay(transit)`, `AllNight(transit)`.
- The library has a known limitation at borderline latitudes: for Reykjavik (64.1466°N) at mid-summer, it returns `RegularDay` instead of `AllDay`, but the `sunrise` and `sunset` instants are degenerate (`sunrise=2026-06-21T02:55 UTC, sunset=2026-06-21T00:03 UTC` — sunset is BEFORE sunrise). Verified by running the library directly with these coordinates.
- 2.0.13 has the same bug (verified). It's a library-side issue, not version-specific.
- The test was originally named `returnsEmptyForReykjavikMidSummer` and asserted `isEmpty()`, but commit `7e11cd9` (M4 day) renamed it to `returnsResultForReykjavikMidSummer` with `isPresent()` to sidestep the failure. M4 said "leave it alone" — we're undoing that workaround as part of this plan.
- The right application-layer fix: validate `sunrise.isBefore(sunset)` for the `RegularDay` branch. If not, treat the result as effectively all-day/all-night and return `Optional.empty()`.

- [ ] **Step 1: Restore the original test name and assertion**

  Edit `ComputedSunTimesProviderTest.java`:
  - Rename `returnsResultForReykjavikMidSummer` back to `returnsEmptyForReykjavikMidSummer`.
  - Change `assertThat(result).isPresent();` to `assertThat(result).isEmpty();`.
  - Update the comment to: "Reykjavik at 64.1°N is borderline polar day. solarpositioning returns RegularDay with degenerate times (sunset before sunrise); we treat that as effectively all-day."

  Run: `mvn -pl weather-infrastructure test -Dtest=ComputedSunTimesProviderTest#returnsEmptyForReykjavikMidSummer`
  Expected: FAIL. Confirms the library bug surfaces through the reflection-based code.

- [ ] **Step 2: Replace the reflection with the sealed switch + degenerate-day check**

  Replace the body of `getSunTimes(...)` in `ComputedSunTimesProvider.java`:

  ```java
  Timer.Sample sample = Timer.start(meterRegistry);
  try {
      ZonedDateTime noon = date.atTime(12, 0).atZone(zone);

      SunriseResult result = SPA.calculateSunriseTransitSet(
              noon, location.latitude(), location.longitude(), SUNRISE_SUNSET_ZENITH);

      sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "success"));

      return switch (result) {
          // Library bug: at borderline polar latitudes (~64°N at solstice),
          // solarpositioning returns RegularDay with sunrise > sunset. Treat
          // those as all-day / all-night and surface empty.
          case SunriseResult.RegularDay d when d.sunrise().isBefore(d.sunset()) ->
              Optional.of(new SunTimes(
                      date, d.sunrise().toInstant(), d.sunset().toInstant(), zone.getId()));
          case SunriseResult.RegularDay d, SunriseResult.AllDay d, SunriseResult.AllNight d ->
              Optional.empty();
      };
  } catch (Exception e) {
      sample.stop(meterRegistry.timer(TIMER_NAME, "outcome", "failure"));
      return Optional.empty();
  }
  ```

  Also drop the unused `import java.lang.reflect.Method` etc. if any remain. Keep the existing `import net.e175.klaus.solarpositioning.SunriseResult;` (it was already imported via `SunriseResult result`).

- [ ] **Step 3: Run the Reykjavik test**

  Run: `mvn -pl weather-infrastructure test -Dtest=ComputedSunTimesProviderTest#returnsEmptyForReykjavikMidSummer`
  Expected: PASS.

- [ ] **Step 4: Run the full provider tests**

  Run: `mvn -pl weather-infrastructure test`
  Expected: all 3 tests pass (was 3 passing the wrong thing + 1 hidden failure; now 3 passing the right thing).

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/sun/ComputedSunTimesProvider.java \
          backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/sun/ComputedSunTimesProviderTest.java
  git commit -m "fix(backend): drop reflection, restore polar-day boundary check in ComputedSunTimesProvider"
  ```

---

### Task B8 — `RestClientConfig` dedup

**Files:**
- Modify: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/config/RestClientConfig.java`

- [ ] **Step 1: Extract private builder helper**

  Add:
  ```java
  private RestClient build(String baseUrl, String userAgent) {
      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
      return RestClient.builder()
              .requestFactory(factory)
              .baseUrl(baseUrl)
              .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
              .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .build();
  }
  ```

  Replace each `@Bean` method body with `return build(p.getBaseUrl(), p.getUserAgent());` and similar for Nominatim.

- [ ] **Step 2: Delete the duplicate Javadoc block**

  Remove the second identical Javadoc block (the one at lines 22-30 in the current file).

- [ ] **Step 3: Run tests**

  Run: `mvn -pl weather-infrastructure test`
  Expected: all pass.

- [ ] **Step 4: Commit**

  ```bash
  git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/config/RestClientConfig.java
  git commit -m "refactor(backend): dedup RestClientConfig beans and Javadoc"
  ```

---

### Task B9 — `WeatherForecast` / `HourlyForecast` — investigated, kept split

**Files:**
- Modify: `backend/weather-domain/.../model/WeatherForecast.java`
- Modify: `backend/weather-domain/.../model/HourlyForecast.java`

- [ ] **Step 1: Inspect both records**

  Read both files. Confirm:
  - Do `WeatherForecast` and `HourlyForecast` share the same fields (`generatedAt`, `source`, `periods`/`hours`)?
  - Are the period element types (`ForecastPeriod` vs `HourlyForecastPeriod`) compatible?

- [ ] **Step 2: If mergeable → generic `GeneratedForecast<P>`**

  If they can be merged without behavior change, create `GeneratedForecast<P extends ForecastPeriodLike>` and delete the two old records. Update the 2 use cases that use them.

- [ ] **Step 3: If not mergeable → comment and skip**

  Add `# ponytail: candidates for merge with WeatherForecast; kept split because <period-type differs / source semantics differ / fields don't line up>` to both files. No code change.

- [ ] **Step 4: Commit (only if merged)**

  If merged: `refactor(backend): merge WeatherForecast and HourlyForecast into GeneratedForecast<P>`.

  If comment-only: `docs(backend): note WeatherForecast/HourlyForecast merge evaluation`.

---

## Acceptance

- `mvn clean test` passes with zero failures (was 1 failure: `returnsEmptyForReykjavikMidSummer`).
- 3 NWS providers no longer duplicate `/points` lookup.
- 4 NWS providers no longer duplicate HTTP-error translation.
- `Location.cacheKey(String)` replaces 3 hand-rolled methods.
- `WeatherQueryResult` is deleted.
- `ComputedSunTimesProvider` uses a sealed switch with no reflection.
- `RestClientConfig` has one private builder and one Javadoc block.
- Net diff: ~300 LOC removed, one previously-failing test now passes.
