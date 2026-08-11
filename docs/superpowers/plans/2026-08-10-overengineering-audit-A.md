# Plan A — Generic Cache Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace 6 cache ports + 7 Redis cache adapters + 6 use-case cache-aside duplicates with one generic `Cache<V>` port, one `RedisJsonCache<V>` adapter, and one `CacheAside` helper. Eliminates ~600 LOC across 3 Maven modules.

**Architecture:** New `Cache<V>` interface in domain layer (single method contract). New `RedisJsonCache<V>` in infrastructure layer (handles JSON serialization + hit/miss metrics). New `CacheAside.getOrLoad(...)` static helper in application layer. All 6 existing cache-specific ports and 7 adapter classes are deleted; the 6 use cases are migrated to depend on the generic `Cache<V>` parameterized by their value type.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JUnit 5, AssertJ, Testcontainers Redis.

## Global Constraints

- See [`2026-08-10-overengineering-audit.md`](./2026-08-10-overengineering-audit.md) global constraints.
- Cache key prefixes must remain identical to the current values to preserve cache compatibility in deployed environments: `weather:`, `hourly:`, `obs:`, `alerts:`, `afd:`, `geo:`, `sun:`.
- Hit/miss metric names must remain identical: `weather.cache.{prefix}.hits` and `weather.cache.{prefix}.misses`.
- The `afd:` cache stores raw text (String), not JSON — the new `RedisJsonCache<String>` must handle String→JSON serialization (Jackson serializes a String as a quoted JSON string, which round-trips losslessly).

## Files

**Create:**
- `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/Cache.java`
- `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisJsonCache.java`
- `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/CacheAside.java`
- Test files for each: `CacheTest.java`, `RedisJsonCacheTest.java`, `CacheAsideTest.java`

**Delete (after migration):**
- 7 ports: `WeatherCache`, `HourlyForecastCache`, `AlertCache`, `ObservationCache`, `SunTimesCache`, `AfdCache`, `LocationCache`
- 7 `backend/weather-infrastructure/.../cache/Redis*Cache.java` adapters
- 7 `backend/weather-infrastructure/.../cache/Redis*CacheTest.java` test classes

**Modify:**
- 7 use cases / resolvers (`GetWeatherUseCase`, `GetHourlyForecastUseCase`, `GetCurrentConditionsUseCase`, `GetActiveAlertsUseCase`, `GetAfdUseCase`, `GetSunTimesUseCase`, `LocationResolver`)
- `backend/weather-api/.../config/UseCaseConfig.java` (wire new `Cache<V>` beans)

---

### Task A1 — Introduce `Cache<V>` port (TDD)

**Files:**
- Create: `backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/Cache.java`
- Create: `backend/weather-domain/src/test/java/io/ythalorossy/weatherapi/domain/port/CacheTest.java`

- [ ] **Step 1: Write failing test**

  `CacheTest.java`:
  ```java
  package io.ythalorossy.weatherapi.domain.port;

  import org.junit.jupiter.api.Test;
  import java.time.Duration;
  import java.util.Optional;
  import static org.assertj.core.api.Assertions.assertThat;

  class CacheTest {

      @Test
      void genericPortExposesGetAndPut() {
          // Compile-time assertion: a Cache<String> has get(String) returning
          // Optional<String> and put(String, String, Duration).
          Cache<String> cache = new Cache<>() {
              @Override public Optional<String> get(String key) { return Optional.of(key); }
              @Override public void put(String key, String value, Duration ttl) {}
          };
          assertThat(cache.get("k")).contains("k");
      }
  }
  ```

- [ ] **Step 2: Run test, expect failure (no `Cache` type)**

  Run: `mvn -pl weather-domain test -Dtest=CacheTest`
  Expected: compile error `cannot find symbol: class Cache`.

- [ ] **Step 3: Implement the port**

  `Cache.java`:
  ```java
  package io.ythalorossy.weatherapi.domain.port;

  import java.time.Duration;
  import java.util.Optional;

  public interface Cache<V> {
      Optional<V> get(String key);
      void put(String key, V value, Duration ttl);
  }
  ```

- [ ] **Step 4: Run test, expect pass**

  Run: `mvn -pl weather-domain test -Dtest=CacheTest`
  Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/Cache.java \
          backend/weather-domain/src/test/java/io/ythalorossy/weatherapi/domain/port/CacheTest.java
  git commit -m "refactor(backend): introduce generic Cache<V> port"
  ```

---

### Task A2 — Implement `RedisJsonCache<V>` adapter (TDD)

**Files:**
- Create: `backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisJsonCache.java`
- Create: `backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisJsonCacheTest.java`

- [ ] **Step 1: Write failing test**

  `RedisJsonCacheTest.java`:
  ```java
  package io.ythalorossy.weatherapi.infrastructure.cache;

  import com.fasterxml.jackson.databind.ObjectMapper;
  import io.ythalorossy.weatherapi.domain.model.Location;
  import io.ythalorossy.weatherapi.infrastructure.InfrastructureTestConfig;
  import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
  import org.junit.jupiter.api.AfterAll;
  import org.junit.jupiter.api.BeforeAll;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
  import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
  import org.springframework.data.redis.core.StringRedisTemplate;
  import org.springframework.test.context.ContextConfiguration;
  import org.springframework.test.context.DynamicPropertyRegistry;
  import org.springframework.test.context.DynamicPropertySource;
  import org.testcontainers.containers.GenericContainer;
  import org.testcontainers.utility.DockerImageName;

  import java.time.Duration;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;

  @SpringBootTest(classes = InfrastructureTestConfig.class)
  @ContextConfiguration(classes = InfrastructureTestConfig.class)
  class RedisJsonCacheTest {

      static GenericContainer<?> REDIS =
              new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                      .withExposedPorts(6379);
      static RedisJsonCache<Location> cache;

      @DynamicPropertySource
      static void redisProps(DynamicPropertyRegistry r) {
          REDIS.start();
          r.add("spring.data.redis.host", REDIS::getHost);
          r.add("spring.data.redis.port", REDIS::getFirstMappedPort);
      }

      @BeforeAll
      static void setup(@Autowired StringRedisTemplate template) {
          cache = new RedisJsonCache<>(
                  template, new ObjectMapper(), "geo",
                  Location.class, new SimpleMeterRegistry());
      }

      @AfterAll
      static void teardown() { REDIS.stop(); }

      @Test
      void roundTripsValueThroughRedis() {
          Location value = new Location(38.88, -77.09, "Arlington, VA");
          cache.put("arlington-va", value, Duration.ofMinutes(1));

          Optional<Location> hit = cache.get("arlington-va");
          assertThat(hit).isPresent();
          assertThat(hit.get().latitude()).isEqualTo(38.88);
          assertThat(hit.get().displayName()).isEqualTo("Arlington, VA");
      }

      @Test
      void missingKeyReturnsEmpty() {
          Optional<Location> miss = cache.get("does-not-exist");
          assertThat(miss).isEmpty();
      }
  }
  ```

- [ ] **Step 2: Run test, expect failure (no `RedisJsonCache` type)**

  Run: `mvn -pl weather-infrastructure test -Dtest=RedisJsonCacheTest`
  Expected: compile error `cannot find symbol: class RedisJsonCache`.

- [ ] **Step 3: Implement the adapter**

  `RedisJsonCache.java`:
  ```java
  package io.ythalorossy.weatherapi.infrastructure.cache;

  import com.fasterxml.jackson.core.JsonProcessingException;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import io.micrometer.core.instrument.Counter;
  import io.micrometer.core.instrument.MeterRegistry;
  import io.ythalorossy.weatherapi.domain.port.Cache;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.data.redis.core.StringRedisTemplate;

  import java.time.Duration;
  import java.util.Objects;
  import java.util.Optional;

  public class RedisJsonCache<V> implements Cache<V> {

      private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);

      private final StringRedisTemplate redis;
      private final ObjectMapper mapper;
      private final String prefix;
      private final Class<V> type;
      private final Counter hits;
      private final Counter misses;

      public RedisJsonCache(StringRedisTemplate redis, ObjectMapper mapper,
                            String prefix, Class<V> type, MeterRegistry meters) {
          this.redis = Objects.requireNonNull(redis);
          this.mapper = Objects.requireNonNull(mapper);
          this.prefix = Objects.requireNonNull(prefix);
          this.type = Objects.requireNonNull(type);
          this.hits = Counter.builder("weather.cache." + prefix + ".hits")
              .description("Cache hits for " + prefix + " namespace")
              .register(meters);
          this.misses = Counter.builder("weather.cache." + prefix + ".misses")
              .description("Cache misses for " + prefix + " namespace")
              .register(meters);
      }

      @Override
      public Optional<V> get(String key) {
          Objects.requireNonNull(key, "key");
          try {
              String json = redis.opsForValue().get(prefix + ":" + key);
              if (json == null) {
                  misses.increment();
                  return Optional.empty();
              }
              hits.increment();
              return Optional.of(mapper.readValue(json, type));
          } catch (JsonProcessingException e) {
              log.warn("Failed to deserialize {} cache key '{}': {}",
                       prefix, key, e.getMessage());
              misses.increment();
              return Optional.empty();
          } catch (Exception e) {
              log.warn("Redis GET failed for {} cache key '{}': {}",
                       prefix, key, e.getMessage());
              misses.increment();
              return Optional.empty();
          }
      }

      @Override
      public void put(String key, V value, Duration ttl) {
          Objects.requireNonNull(key, "key");
          Objects.requireNonNull(value, "value");
          Objects.requireNonNull(ttl, "ttl");
          if (ttl.isZero() || ttl.isNegative()) {
              throw new IllegalArgumentException("ttl must be positive: " + ttl);
          }
          try {
              String json = mapper.writeValueAsString(value);
              redis.opsForValue().set(prefix + ":" + key, json, ttl);
          } catch (JsonProcessingException e) {
              log.warn("Failed to serialize {} cache key '{}': {}",
                       prefix, key, e.getMessage());
          } catch (Exception e) {
              log.warn("Redis SET failed for {} cache key '{}': {}",
                       prefix, key, e.getMessage());
          }
      }
  }
  ```

- [ ] **Step 4: Run test, expect pass**

  Run: `mvn -pl weather-infrastructure test -Dtest=RedisJsonCacheTest`
  Expected: `Tests run: 2, Failures: 0`.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisJsonCache.java \
          backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisJsonCacheTest.java
  git commit -m "refactor(backend): add RedisJsonCache<V> generic adapter"
  ```

---

### Task A3 — Extract `CacheAside` helper (TDD)

**Files:**
- Create: `backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/CacheAside.java`
- Create: `backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/CacheAsideTest.java`

- [ ] **Step 1: Write failing test**

  `CacheAsideTest.java`:
  ```java
  package io.ythalorossy.weatherapi.application.usecase;

  import io.ythalorossy.weatherapi.domain.port.Cache;
  import org.junit.jupiter.api.Test;
  import java.time.Duration;
  import java.util.Optional;
  import java.util.concurrent.atomic.AtomicInteger;
  import static org.assertj.core.api.Assertions.*;

  class CacheAsideTest {

      @Test
      void returnsCachedValueOnHit() {
          Cache<String> cache = new InMemoryCache();
          cache.put("k", "cached", Duration.ofMinutes(1));
          AtomicInteger calls = new AtomicInteger();

          String v = CacheAside.getOrLoad(cache, "k", Duration.ofMinutes(1),
                  () -> { calls.incrementAndGet(); return "loaded"; });

          assertThat(v).isEqualTo("cached");
          assertThat(calls).hasValue(0);
      }

      @Test
      void loadsAndCachesOnMiss() {
          Cache<String> cache = new InMemoryCache();
          AtomicInteger calls = new AtomicInteger();

          String v = CacheAside.getOrLoad(cache, "k", Duration.ofMinutes(1),
                  () -> { calls.incrementAndGet(); return "loaded"; });

          assertThat(v).isEqualTo("loaded");
          assertThat(calls).hasValue(1);
          assertThat(cache.get("k")).contains("loaded");
      }

      @Test
      void propagatesSupplierExceptions() {
          Cache<String> cache = new InMemoryCache();
          assertThatThrownBy(() -> CacheAside.getOrLoad(cache, "k",
                  Duration.ofMinutes(1), () -> { throw new IllegalStateException("boom"); }))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("boom");
      }

      @Test
      void rejectsNonPositiveTtl() {
          CacheAside.requirePositive(Duration.ZERO, "ttl");
          assertThatThrownBy(() -> CacheAside.requirePositive(Duration.ofSeconds(-1), "ttl"))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("ttl must be positive");
      }

      static class InMemoryCache implements Cache<String> {
          private final java.util.Map<String, String> map = new java.util.HashMap<>();
          @Override public Optional<String> get(String key) { return Optional.ofNullable(map.get(key)); }
          @Override public void put(String key, String value, Duration ttl) { map.put(key, value); }
      }
  }
  ```

- [ ] **Step 2: Run test, expect failure (no `CacheAside` class)**

  Run: `mvn -pl weather-application test -Dtest=CacheAsideTest`
  Expected: compile error `cannot find symbol: class CacheAside`.

- [ ] **Step 3: Implement**

  `CacheAside.java`:
  ```java
  package io.ythalorossy.weatherapi.application.usecase;

  import io.ythalorossy.weatherapi.domain.port.Cache;
  import java.time.Duration;
  import java.util.function.Supplier;

  public final class CacheAside {

      private CacheAside() {}

      public static <V> V getOrLoad(Cache<V> cache, String key, Duration ttl,
                                     Supplier<V> loader) {
          return cache.get(key).orElseGet(() -> {
              V value = loader.get();
              cache.put(key, value, ttl);
              return value;
          });
      }

      public static void requirePositive(Duration ttl, String name) {
          if (ttl.isZero() || ttl.isNegative()) {
              throw new IllegalArgumentException(name + " must be positive: " + ttl);
          }
      }
  }
  ```

- [ ] **Step 4: Run test, expect pass**

  Run: `mvn -pl weather-application test -Dtest=CacheAsideTest`
  Expected: `Tests run: 4, Failures: 0`.

- [ ] **Step 5: Commit**

  ```bash
  git add backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/CacheAside.java \
          backend/weather-application/src/test/java/io/ythalorossy/weatherapi/application/usecase/CacheAsideTest.java
  git commit -m "refactor(backend): extract CacheAside helper"
  ```

---

### Task A4 — Migrate first use case (Weather) end-to-end

**Files:**
- Modify: `backend/weather-application/.../usecase/GetWeatherUseCase.java`
- Modify: `backend/weather-api/.../config/UseCaseConfig.java`
- Delete: `backend/weather-domain/.../port/WeatherCache.java`
- Delete: `backend/weather-infrastructure/.../cache/RedisWeatherCache.java`
- Delete: `backend/weather-infrastructure/.../cache/RedisWeatherCacheTest.java`

- [ ] **Step 1: Inspect the existing `GetWeatherUseCase`**

  Run `cat backend/weather-application/src/main/java/io/ythalorossy/weatherapi/application/usecase/GetWeatherUseCase.java` to see the current cache-aside pattern and TTL constant.

- [ ] **Step 2: Update the constructor and method body**

  Replace `WeatherCache weatherCache` constructor parameter with `Cache<WeatherForecast> weatherCache`. Replace the inline cache-aside logic with `CacheAside.getOrLoad(weatherCache, key, TTL, () -> provider.getForecast(location))`. Remove the local `requirePositive` private method (replaced by `CacheAside.requirePositive`).

- [ ] **Step 3: Wire the new bean in `UseCaseConfig`**

  Add:
  ```java
  @Bean
  Cache<WeatherForecast> weatherCache(StringRedisTemplate redis, ObjectMapper mapper,
                                       MeterRegistry meters) {
      return new RedisJsonCache<>(redis, mapper, "weather", WeatherForecast.class, meters);
  }
  ```

- [ ] **Step 4: Delete the old port, adapter, and test**

  ```bash
  rm backend/weather-domain/src/main/java/io/ythalorossy/weatherapi/domain/port/WeatherCache.java
  rm backend/weather-infrastructure/src/main/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisWeatherCache.java
  rm backend/weather-infrastructure/src/test/java/io/ythalorossy/weatherapi/infrastructure/cache/RedisWeatherCacheTest.java
  ```

- [ ] **Step 5: Run tests, expect pass**

  Run: `mvn -pl weather-api test`
  Expected: 25 tests pass (unchanged from before this task).

- [ ] **Step 6: Commit**

  ```bash
  git add backend/weather-application backend/weather-api backend/weather-domain backend/weather-infrastructure
  git commit -m "refactor(backend): migrate GetWeatherUseCase to Cache<WeatherForecast>"
  ```

---

### Task A5 — Migrate `GetHourlyForecastUseCase`

- [ ] Repeat Task A4 with `Cache<HourlyForecast>` and prefix `hourly`. Delete `HourlyForecastCache.java`, `RedisHourlyForecastCache.java`, `RedisHourlyForecastCacheTest.java`.
- [ ] `mvn -pl weather-api test` passes.
- [ ] Commit `refactor(backend): migrate GetHourlyForecastUseCase to Cache<HourlyForecast>`.

---

### Task A6 — Migrate `GetCurrentConditionsUseCase`

- [ ] Repeat Task A4 with `Cache<Observation>` and prefix `obs`. Delete `ObservationCache.java`, `RedisObservationCache.java`, `RedisObservationCacheTest.java`.
- [ ] `mvn -pl weather-api test` passes.
- [ ] Commit `refactor(backend): migrate GetCurrentConditionsUseCase to Cache<Observation>`.

---

### Task A7 — Migrate `GetActiveAlertsUseCase`

- [ ] Repeat Task A4 with `Cache<List<WeatherAlert>>` and prefix `alerts`. Delete `AlertCache.java`, `RedisAlertCache.java`, `RedisAlertCacheTest.java`.
- [ ] `mvn -pl weather-api test` passes.
- [ ] Commit `refactor(backend): migrate GetActiveAlertsUseCase to Cache<List<WeatherAlert>>`.

---

### Task A8 — Migrate `GetAfdUseCase`

- [ ] Repeat Task A4 with `Cache<String>` (text) and prefix `afd`. Delete `AfdCache.java`, `RedisAfdCache.java`, `RedisAfdCacheTest.java`.
- [ ] `mvn -pl weather-api test` passes.
- [ ] Commit `refactor(backend): migrate GetAfdUseCase to Cache<String>`.

---

### Task A9 — Migrate `GetSunTimesUseCase`

- [ ] Repeat Task A4 with `Cache<SunTimes>` and prefix `sun`. Delete `SunTimesCache.java`, `RedisSunTimesCache.java`, `RedisSunTimesCacheTest.java`.
- [ ] `mvn -pl weather-api test` passes.
- [ ] Commit `refactor(backend): migrate GetSunTimesUseCase to Cache<SunTimes>`.

---

### Task A10 — Migrate `LocationResolver` (geo cache)

- [ ] Repeat Task A4 with `Cache<Location>` and prefix `geo`. Delete `LocationCache.java`, `RedisLocationCache.java`, `RedisLocationCacheTest.java`.
- [ ] `mvn -pl weather-api test` passes.
- [ ] Commit `refactor(backend): migrate LocationResolver to Cache<Location>`.

---

### Task A11 — Final cleanup

- [ ] **Step 1: Confirm no stale references**

  Run: `grep -rn "WeatherCache\|HourlyForecastCache\|AlertCache\|ObservationCache\|SunTimesCache\|AfdCache\|LocationCache\|RedisWeatherCache\|RedisHourlyForecastCache\|RedisAlertCache\|RedisObservationCache\|RedisSunTimesCache\|RedisAfdCache\|RedisLocationCache" backend/`
  Expected: no output.

- [ ] **Step 2: Remove unused imports across all use cases**

  Run `mvn -pl weather-api compile -Dmaven.compiler.showDeprecation=true` to surface any leftover imports.

- [ ] **Step 3: Full reactor test**

  Run: `mvn clean test`
  Expected: ~94 tests passing (was 92, -7 deleted cache tests, +1 CacheTest, +3 CacheAsideTest, +2 RedisJsonCacheTest, +3 new UseCase tests = ~94).

- [ ] **Step 4: Commit**

  ```bash
  git add backend/
  git commit -m "refactor(backend): remove deprecated cache ports and adapters"
  ```

---

## Acceptance

- `mvn clean test` passes with ~91-95 tests (was 92; -7 deleted cache tests + 6 new helper/port tests).
- No `Cache<V>` call site still depends on a typed cache port.
- 7 `Redis*Cache.java` and 7 `Redis*CacheTest.java` files are deleted from disk and git.
- Net diff: ~600 LOC removed across `weather-domain` + `weather-infrastructure`.
